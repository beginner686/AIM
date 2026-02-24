package com.ailink.module.order.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class FeeConfigVO {

    private BigDecimal platformFeeRate;
    private BigDecimal escrowFeeRate;
}
