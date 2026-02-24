package com.ailink.module.stat.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProfitSummaryVO {

    private BigDecimal totalAmount;
    private BigDecimal totalPlatformFee;
    private BigDecimal totalEscrowFee;
    private Long orderCount;
}
