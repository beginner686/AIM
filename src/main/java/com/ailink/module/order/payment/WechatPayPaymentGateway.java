package com.ailink.module.order.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WechatPayPaymentGateway implements PaymentGateway {

    @Override
    public String channel() {
        return "WECHAT_PAY";
    }

    @Override
    public void hold(String bizOrderNo, BigDecimal amount) {
        // Reserve Wechat Pay integration point.
    }
}
