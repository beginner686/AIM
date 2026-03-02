package com.ailink.module.order.payment;

import com.ailink.common.exception.BizException;
import com.ailink.config.WechatPayProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WechatNativePayServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRejectNotifyWhenMchIdMismatch() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        KeyPair keyPair = generateRsaKeyPair();

        Path merchantPrivateKeyPath = tempDir.resolve("merchant_private.pem");
        Path platformPublicKeyPath = tempDir.resolve("platform_public.pem");
        writePem(merchantPrivateKeyPath, "PRIVATE KEY", keyPair.getPrivate().getEncoded());
        writePem(platformPublicKeyPath, "PUBLIC KEY", keyPair.getPublic().getEncoded());

        WechatPayProperties properties = new WechatPayProperties();
        properties.setEnabled(true);
        properties.setAppId("wx-test-app");
        properties.setMchId("mch-expected");
        properties.setMerchantSerialNo("serial-001");
        properties.setPrivateKeyPath(merchantPrivateKeyPath.toString());
        properties.setPlatformPublicKeyPath(platformPublicKeyPath.toString());
        properties.setApiV3Key("0123456789abcdef0123456789abcdef");
        properties.setNotifyUrl("https://example.com/api/payment/wechat/notify");

        WechatNativePayService service = new WechatNativePayService(
                properties,
                new RestTemplate(),
                objectMapper,
                new DefaultResourceLoader());

        String plainJson = objectMapper.writeValueAsString(buildTransactionPayload(
                "mch-illegal",
                properties.getAppId(),
                "PAY123456789",
                "WXTX123456",
                "SUCCESS",
                100,
                "CNY"));
        String associatedData = "transaction";
        String nonceText = "abcdef123456";
        String cipherText = encryptAesGcm(properties.getApiV3Key(), nonceText, associatedData, plainJson);

        Map<String, Object> notifyPayload = new LinkedHashMap<>();
        notifyPayload.put("id", "notif-001");
        notifyPayload.put("event_type", "TRANSACTION.SUCCESS");
        notifyPayload.put("resource", Map.of(
                "associated_data", associatedData,
                "nonce", nonceText,
                "ciphertext", cipherText));
        String body = objectMapper.writeValueAsString(notifyPayload);

        String timestamp = "1710000000";
        String nonce = "nonce-123";
        String signature = signWithRsa(
                keyPair.getPrivate(),
                timestamp + "\n" + nonce + "\n" + body + "\n");

        BizException exception = assertThrows(BizException.class,
                () -> service.verifyAndParseNotify(body, timestamp, nonce, signature));
        assertTrue(exception.getMessage().contains("merchant validation failed"));
    }

    private static Map<String, Object> buildTransactionPayload(String mchId,
                                                               String appId,
                                                               String outTradeNo,
                                                               String txId,
                                                               String tradeState,
                                                               int totalFeeFen,
                                                               String currency) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("mchid", mchId);
        payload.put("appid", appId);
        payload.put("out_trade_no", outTradeNo);
        payload.put("transaction_id", txId);
        payload.put("trade_state", tradeState);
        payload.put("amount", Map.of("total", totalFeeFen, "currency", currency));
        return payload;
    }

    private static String encryptAesGcm(String apiV3Key, String nonce, String associatedData, String plainText)
            throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec keySpec = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
        if (associatedData != null && !associatedData.isEmpty()) {
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        }
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private static String signWithRsa(PrivateKey privateKey, String message) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static void writePem(Path path, String type, byte[] encodedKey) throws Exception {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(encodedKey);
        String pem = "-----BEGIN " + type + "-----\n"
                + base64 + "\n"
                + "-----END " + type + "-----\n";
        Files.writeString(path, pem, StandardCharsets.UTF_8);
    }
}
