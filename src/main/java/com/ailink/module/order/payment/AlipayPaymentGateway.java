package com.ailink.module.order.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AlipayPaymentGateway implements PaymentGateway {

    @Override
    public String channel() {
        return "ALIPAY";
    }

    @Override
    public void hold(String bizOrderNo, BigDecimal amount) {
        // Reserve Alipay integration point.
    }
}
