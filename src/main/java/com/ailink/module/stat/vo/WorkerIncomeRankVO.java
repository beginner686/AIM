package com.ailink.module.stat.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkerIncomeRankVO {

    private Long workerUserId;
    private BigDecimal totalIncome;
    private Long orderCount;
}
