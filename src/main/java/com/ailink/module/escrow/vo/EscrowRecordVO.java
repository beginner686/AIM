package com.ailink.module.escrow.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EscrowRecordVO {

    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private String channel;
    private String status;
    private String transactionNo;
    private LocalDateTime releasedTime;
    private LocalDateTime createdTime;
}
