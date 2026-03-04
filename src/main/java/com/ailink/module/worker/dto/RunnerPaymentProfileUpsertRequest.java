package com.ailink.module.worker.dto;

import lombok.Data;

@Data
public class RunnerPaymentProfileUpsertRequest {

    private String paypalEmail;
    private String wiseId;
    private String wiseLink;
    private String payoneerLink;
    private String cryptoWallet;
    private String paymentUrl;
    private String currency;
}
