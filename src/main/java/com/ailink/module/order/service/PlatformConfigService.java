package com.ailink.module.order.service;

import com.ailink.module.order.vo.FeeConfigVO;

import java.math.BigDecimal;

public interface PlatformConfigService {

    String PLATFORM_FEE_RATE_KEY = "platform_fee_rate";
    String ESCROW_FEE_RATE_KEY = "escrow_fee_rate";

    BigDecimal getDecimal(String key, BigDecimal defaultValue);

    FeeConfigVO getFeeConfig();

    void updateFeeConfig(BigDecimal platformFeeRate, BigDecimal escrowFeeRate);
}
