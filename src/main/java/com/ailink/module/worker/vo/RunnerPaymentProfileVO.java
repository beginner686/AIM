package com.ailink.module.worker.vo;

import lombok.Data;

@Data
public class RunnerPaymentProfileVO {

    private Long id;
    private Long userId;
    private String paypalEmail;
    private String wiseLink;
    private String paymentUrl;
    private String currency;
    private Integer verified;
}
