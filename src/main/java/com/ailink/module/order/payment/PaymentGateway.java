package com.ailink.module.order.payment;

import java.math.BigDecimal;

public interface PaymentGateway {

    String channel();

    void hold(String bizOrderNo, BigDecimal amount);
}
