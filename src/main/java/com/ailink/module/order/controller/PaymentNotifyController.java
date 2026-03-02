package com.ailink.module.order.controller;

import com.ailink.module.order.service.ServiceFeeService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment/wechat")
public class PaymentNotifyController {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotifyController.class);

    private final ServiceFeeService serviceFeeService;

    public PaymentNotifyController(ServiceFeeService serviceFeeService) {
        this.serviceFeeService = serviceFeeService;
    }

    @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> notify(@RequestBody String body, HttpServletRequest request) {
        String signature = request.getHeader("Wechatpay-Signature");
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String serial = request.getHeader("Wechatpay-Serial");
        log.info("wechat notify received, serial={}, timestamp={}, nonce={}", serial, timestamp, nonce);
        try {
            String responseBody = serviceFeeService.handleWechatNotify(signature, timestamp, nonce, body);
            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            log.error("wechat notify handle failed", e);
            return ResponseEntity.status(HttpStatus.OK)
                    .body("{\"code\":\"FAIL\",\"message\":\"callback process failed\"}");
        }
    }
}
