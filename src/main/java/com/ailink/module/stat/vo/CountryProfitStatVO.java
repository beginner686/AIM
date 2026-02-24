package com.ailink.module.stat.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CountryProfitStatVO {

    private String country;
    private BigDecimal totalAmount;
    private BigDecimal totalPlatformFee;
    private Long orderCount;
}
