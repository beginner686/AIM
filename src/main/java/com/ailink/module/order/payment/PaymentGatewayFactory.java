package com.ailink.module.order.payment;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentGatewayFactory {

    private final List<PaymentGateway> paymentGateways;

    public PaymentGatewayFactory(List<PaymentGateway> paymentGateways) {
        this.paymentGateways = paymentGateways;
    }

    public PaymentGateway getOrDefault(String channel) {
        if (channel == null) {
            return defaultGateway();
        }
        return paymentGateways.stream()
                .filter(gateway -> channel.equalsIgnoreCase(gateway.channel()))
                .findFirst()
                .orElse(defaultGateway());
    }

    private PaymentGateway defaultGateway() {
        return paymentGateways.stream()
                .filter(gateway -> "MANUAL".equalsIgnoreCase(gateway.channel()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("MANUAL payment gateway is required"));
    }
}
