package com.ailink.module.order.dto;

import lombok.Data;

@Data
public class ServiceFeePayRequest {

    private String paymentChannel;
    private String remark;
}
