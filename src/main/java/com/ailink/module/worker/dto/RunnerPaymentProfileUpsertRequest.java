package com.ailink.module.worker.dto;

import lombok.Data;

@Data
public class RunnerPaymentProfileUpsertRequest {

    private String paypalEmail;
    private String wiseLink;
    private String paymentUrl;
    private String currency;
}
