package com.ailink.module.order.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ManualPaymentGateway implements PaymentGateway {

    @Override
    public String channel() {
        return "MANUAL";
    }

    @Override
    public void hold(String bizOrderNo, BigDecimal amount) {
        // Placeholder implementation to reserve payment abstraction.
    }
}
