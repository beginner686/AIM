package com.ailink.module.order.payment;

import com.ailink.common.ErrorCode;
import com.ailink.common.exception.BizException;
import com.ailink.config.WechatPayProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WechatNativePayService {

    private static final Logger log = LoggerFactory.getLogger(WechatNativePayService.class);
    private static final String EXPECTED_EVENT_TYPE = "TRANSACTION.SUCCESS";
    private static final String EXPECTED_CURRENCY = "CNY";

    private final WechatPayProperties wechatPayProperties;
    private final RestTemplate wechatPayRestTemplate;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    private volatile PrivateKey merchantPrivateKey;
    private volatile PublicKey platformPublicKey;

    public WechatNativePayService(WechatPayProperties wechatPayProperties,
                                  RestTemplate wechatPayRestTemplate,
                                  ObjectMapper objectMapper,
                                  ResourceLoader resourceLoader) {
        this.wechatPayProperties = wechatPayProperties;
        this.wechatPayRestTemplate = wechatPayRestTemplate;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    public boolean enabled() {
        return wechatPayProperties.isEnabled();
    }

    public WechatNativeOrderResponse createNativeOrder(String outTradeNo, BigDecimal amountYuan, String description) {
        if (!enabled()) {
            WechatNativeOrderResponse mock = new WechatNativeOrderResponse();
            mock.setCodeUrl("mock://wechat-pay/" + outTradeNo);
            return mock;
        }
        ensureRequiredConfig();
        try {
            int totalFen = amountYuan.multiply(new BigDecimal("100"))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValueExact();

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("appid", wechatPayProperties.getAppId());
            requestBody.put("mchid", wechatPayProperties.getMchId());
            requestBody.put("description", StringUtils.hasText(description) ? description : "AI-Link 服务费支付");
            requestBody.put("out_trade_no", outTradeNo);
            requestBody.put("notify_url", wechatPayProperties.getNotifyUrl());
            requestBody.put("amount", Map.of("total", totalFen, "currency", "CNY"));

            String bodyJson = objectMapper.writeValueAsString(requestBody);
            String requestPath = "/v3/pay/transactions/native";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            headers.set("Authorization", buildAuthorizationHeader("POST", requestPath, bodyJson));

            ResponseEntity<String> response = wechatPayRestTemplate.exchange(
                    wechatPayProperties.getBaseUrl() + requestPath,
                    HttpMethod.POST,
                    new HttpEntity<>(bodyJson, headers),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
                throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "wechat native order failed");
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            WechatNativeOrderResponse payResponse = new WechatNativeOrderResponse();
            payResponse.setCodeUrl(root.path("code_url").asText(""));
            payResponse.setPrepayId(root.path("prepay_id").asText(""));
            if (!StringUtils.hasText(payResponse.getCodeUrl())) {
                throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "wechat response missing code_url");
            }
            return payResponse;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("wechat native order request failed, outTradeNo={}", outTradeNo, e);
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "wechat native order failed: " + e.getMessage());
        }
    }

    public WechatNotifyTransaction verifyAndParseNotify(String body,
                                                        String timestamp,
                                                        String nonce,
                                                        String signature) {
        if (!enabled()) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "wechat pay is disabled");
        }
        ensureRequiredConfig();
        if (!verifySignature(timestamp, nonce, body, signature)) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "wechat notify signature verification failed");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode resource = root.path("resource");
            String associatedData = resource.path("associated_data").asText("");
            String cipherText = resource.path("ciphertext").asText("");
            String nonceText = resource.path("nonce").asText("");
            String plainJson = decryptAesGcm(associatedData, nonceText, cipherText);
            JsonNode txRoot = objectMapper.readTree(plainJson);
            validateNotifyPayload(root, txRoot);

            WechatNotifyTransaction tx = new WechatNotifyTransaction();
            tx.setOutTradeNo(txRoot.path("out_trade_no").asText(""));
            tx.setTransactionId(txRoot.path("transaction_id").asText(""));
            tx.setTradeState(txRoot.path("trade_state").asText(""));
            tx.setSuccessTime(txRoot.path("success_time").asText(""));
            tx.setTotalFeeFen(txRoot.path("amount").path("total").asInt(0));
            return tx;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("failed to parse wechat notify payload", e);
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "failed to parse wechat notify payload");
        }
    }

    private void validateNotifyPayload(JsonNode root, JsonNode txRoot) {
        String eventType = root.path("event_type").asText("");
        if (!EXPECTED_EVENT_TYPE.equals(eventType)) {
            log.warn("wechat notify rejected due to event_type mismatch, expected={}, actual={}",
                    EXPECTED_EVENT_TYPE, eventType);
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "wechat notify merchant validation failed");
        }

        String mchId = txRoot.path("mchid").asText("");
        if (!wechatPayProperties.getMchId().equals(mchId)) {
            log.warn("wechat notify rejected due to mchid mismatch, expected={}, actual={}",
                    wechatPayProperties.getMchId(), mchId);
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "wechat notify merchant validation failed");
        }

        String appId = txRoot.path("appid").asText("");
        if (!wechatPayProperties.getAppId().equals(appId)) {
            log.warn("wechat notify rejected due to appid mismatch, expected={}, actual={}",
                    wechatPayProperties.getAppId(), appId);
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "wechat notify merchant validation failed");
        }

        String currency = txRoot.path("amount").path("currency").asText("");
        if (!EXPECTED_CURRENCY.equals(currency)) {
            log.warn("wechat notify rejected due to currency mismatch, expected={}, actual={}",
                    EXPECTED_CURRENCY, currency);
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "wechat notify merchant validation failed");
        }
    }

    private String buildAuthorizationHeader(String method, String requestPath, String body) {
        try {
            String nonceStr = UUID.randomUUID().toString().replace("-", "");
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String message = method + "\n" + requestPath + "\n" + timestamp + "\n" + nonceStr + "\n" + body + "\n";
            String sign = sign(message);
            return "WECHATPAY2-SHA256-RSA2048 "
                    + "mchid=\"" + wechatPayProperties.getMchId() + "\","
                    + "nonce_str=\"" + nonceStr + "\","
                    + "signature=\"" + sign + "\","
                    + "timestamp=\"" + timestamp + "\","
                    + "serial_no=\"" + wechatPayProperties.getMerchantSerialNo() + "\"";
        } catch (Exception e) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "build wechat authorization failed");
        }
    }

    private String sign(String message) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(loadMerchantPrivateKey());
        signature.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private boolean verifySignature(String timestamp, String nonce, String body, String signatureValue) {
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce) || !StringUtils.hasText(signatureValue)) {
            return false;
        }
        try {
            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(loadPlatformPublicKey());
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureValue));
        } catch (Exception e) {
            log.error("verify wechat signature failed", e);
            return false;
        }
    }

    private String decryptAesGcm(String associatedData, String nonce, String cipherText) {
        try {
            byte[] key = wechatPayProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), spec);
            if (StringUtils.hasText(associatedData)) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "decrypt wechat notify payload failed");
        }
    }

    private PrivateKey loadMerchantPrivateKey() throws Exception {
        if (merchantPrivateKey != null) {
            return merchantPrivateKey;
        }
        synchronized (this) {
            if (merchantPrivateKey != null) {
                return merchantPrivateKey;
            }
            String pem = readPemContent(wechatPayProperties.getPrivateKeyPath());
            byte[] keyBytes = Base64.getDecoder().decode(cleanPem(pem));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            merchantPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
            return merchantPrivateKey;
        }
    }

    private PublicKey loadPlatformPublicKey() throws Exception {
        if (platformPublicKey != null) {
            return platformPublicKey;
        }
        synchronized (this) {
            if (platformPublicKey != null) {
                return platformPublicKey;
            }
            String pem = readPemContent(wechatPayProperties.getPlatformPublicKeyPath());
            byte[] keyBytes = Base64.getDecoder().decode(cleanPem(pem));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            platformPublicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
            return platformPublicKey;
        }
    }

    private String readPemContent(String path) throws Exception {
        if (!StringUtils.hasText(path)) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "wechat key path is empty");
        }
        if (path.startsWith("classpath:")) {
            Resource resource = resourceLoader.getResource(path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
        return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
    }

    private String cleanPem(String pem) {
        return pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
    }

    private void ensureRequiredConfig() {
        if (!StringUtils.hasText(wechatPayProperties.getAppId())
                || !StringUtils.hasText(wechatPayProperties.getMchId())
                || !StringUtils.hasText(wechatPayProperties.getMerchantSerialNo())
                || !StringUtils.hasText(wechatPayProperties.getPrivateKeyPath())
                || !StringUtils.hasText(wechatPayProperties.getPlatformPublicKeyPath())
                || !StringUtils.hasText(wechatPayProperties.getApiV3Key())
                || !StringUtils.hasText(wechatPayProperties.getNotifyUrl())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "wechat pay config is incomplete");
        }
    }
}
