package com.ailink.module.order.service.impl;

import com.ailink.module.order.entity.PlatformConfig;
import com.ailink.module.order.mapper.PlatformConfigMapper;
import com.ailink.module.order.service.PlatformConfigService;
import com.ailink.module.order.vo.FeeConfigVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PlatformConfigServiceImpl implements PlatformConfigService {

    private static final BigDecimal DEFAULT_PLATFORM_FEE_RATE = new BigDecimal("0.06");
    private static final BigDecimal DEFAULT_ESCROW_FEE_RATE = new BigDecimal("0.005");

    private final PlatformConfigMapper platformConfigMapper;

    public PlatformConfigServiceImpl(PlatformConfigMapper platformConfigMapper) {
        this.platformConfigMapper = platformConfigMapper;
    }

    @Override
    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        PlatformConfig config = platformConfigMapper.selectOne(new LambdaQueryWrapper<PlatformConfig>()
                .eq(PlatformConfig::getConfigKey, key));
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        return new BigDecimal(config.getConfigValue());
    }

    @Override
    public FeeConfigVO getFeeConfig() {
        BigDecimal platformFeeRate = getDecimal(PLATFORM_FEE_RATE_KEY, DEFAULT_PLATFORM_FEE_RATE);
        BigDecimal escrowFeeRate = getDecimal(ESCROW_FEE_RATE_KEY, DEFAULT_ESCROW_FEE_RATE);
        return new FeeConfigVO(platformFeeRate, escrowFeeRate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFeeConfig(BigDecimal platformFeeRate, BigDecimal escrowFeeRate) {
        saveOrUpdate(PLATFORM_FEE_RATE_KEY, platformFeeRate.stripTrailingZeros().toPlainString(), "platform commission rate");
        saveOrUpdate(ESCROW_FEE_RATE_KEY, escrowFeeRate.stripTrailingZeros().toPlainString(), "escrow handling fee rate");
    }

    private void saveOrUpdate(String key, String value, String desc) {
        PlatformConfig config = platformConfigMapper.selectOne(new LambdaQueryWrapper<PlatformConfig>()
                .eq(PlatformConfig::getConfigKey, key));
        if (config == null) {
            config = new PlatformConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(desc);
            platformConfigMapper.insert(config);
            return;
        }
        config.setConfigValue(value);
        config.setDescription(desc);
        platformConfigMapper.updateById(config);
    }
}
