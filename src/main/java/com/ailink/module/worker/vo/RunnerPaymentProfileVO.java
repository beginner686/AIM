package com.ailink.module.worker.vo;

import lombok.Data;

@Data
public class RunnerPaymentProfileVO {

    private Long id;
    private Long userId;
    private String paypalEmail;
    private String wiseId;
    private String wiseLink;
    private String payoneerLink;
    private String cryptoWallet;
    private String paymentUrl;
    private String currency;
    private Integer verified;
}
