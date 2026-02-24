package com.ailink.module.stat.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CategoryProfitStatVO {

    private String category;
    private BigDecimal totalAmount;
    private BigDecimal totalPlatformFee;
    private Long orderCount;
}
