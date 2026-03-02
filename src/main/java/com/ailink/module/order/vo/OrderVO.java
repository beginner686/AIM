package com.ailink.module.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long demandId;
    private Long employerId;
    private Long workerProfileId;
    private Long workerUserId;
    private BigDecimal amount;
    private String status;
    private String serviceFeeStatus;
    private BigDecimal serviceFeeAmount;
    private LocalDateTime serviceFeePaidTime;
    private String payStatus;
    private String paymentChannel;
    private BigDecimal platformFeeRate;
    private BigDecimal platformFee;
    private BigDecimal escrowFeeRate;
    private BigDecimal escrowFee;
    private BigDecimal workerIncome;
    private String riskStatus;
    private String disputeReason;
    private String closedReason;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
