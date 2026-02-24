package com.ailink.module.order.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StripePaymentGateway implements PaymentGateway {

    @Override
    public String channel() {
        return "STRIPE";
    }

    @Override
    public void hold(String bizOrderNo, BigDecimal amount) {
        // Reserve Stripe integration point.
    }
}
