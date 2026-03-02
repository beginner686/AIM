package com.ailink.module.worker.service.impl;

import com.ailink.common.ErrorCode;
import com.ailink.common.exception.BizException;
import com.ailink.module.worker.dto.RunnerPaymentProfileUpsertRequest;
import com.ailink.module.worker.entity.RunnerPaymentProfile;
import com.ailink.module.worker.mapper.RunnerPaymentProfileMapper;
import com.ailink.module.worker.service.RunnerPaymentProfileService;
import com.ailink.module.worker.vo.RunnerPaymentProfileVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RunnerPaymentProfileServiceImpl implements RunnerPaymentProfileService {

    private final RunnerPaymentProfileMapper runnerPaymentProfileMapper;

    public RunnerPaymentProfileServiceImpl(RunnerPaymentProfileMapper runnerPaymentProfileMapper) {
        this.runnerPaymentProfileMapper = runnerPaymentProfileMapper;
    }

    @Override
    public RunnerPaymentProfileVO getMyProfile(Long userId) {
        return getByUserId(userId);
    }

    @Override
    public RunnerPaymentProfileVO upsertMyProfile(Long userId, String role, RunnerPaymentProfileUpsertRequest request) {
        if (!"WORKER".equalsIgnoreCase(role)) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only runner can edit payment profile");
        }

        RunnerPaymentProfile profile = runnerPaymentProfileMapper.selectOne(new LambdaQueryWrapper<RunnerPaymentProfile>()
                .eq(RunnerPaymentProfile::getUserId, userId));
        if (profile == null) {
            profile = new RunnerPaymentProfile();
            profile.setUserId(userId);
            profile.setVerified(0);
            fill(profile, request);
            runnerPaymentProfileMapper.insert(profile);
            return toVO(profile);
        }

        fill(profile, request);
        runnerPaymentProfileMapper.updateById(profile);
        return toVO(profile);
    }

    @Override
    public RunnerPaymentProfileVO getByUserId(Long userId) {
        RunnerPaymentProfile profile = runnerPaymentProfileMapper.selectOne(new LambdaQueryWrapper<RunnerPaymentProfile>()
                .eq(RunnerPaymentProfile::getUserId, userId));
        if (profile == null) {
            return null;
        }
        return toVO(profile);
    }

    private void fill(RunnerPaymentProfile profile, RunnerPaymentProfileUpsertRequest request) {
        profile.setPaypalEmail(trimToNull(request.getPaypalEmail()));
        profile.setWiseLink(trimToNull(request.getWiseLink()));
        profile.setPaymentUrl(trimToNull(request.getPaymentUrl()));
        profile.setCurrency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency().trim().toUpperCase() : "CNY");
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private RunnerPaymentProfileVO toVO(RunnerPaymentProfile profile) {
        RunnerPaymentProfileVO vo = new RunnerPaymentProfileVO();
        vo.setId(profile.getId());
        vo.setUserId(profile.getUserId());
        vo.setPaypalEmail(profile.getPaypalEmail());
        vo.setWiseLink(profile.getWiseLink());
        vo.setPaymentUrl(profile.getPaymentUrl());
        vo.setCurrency(profile.getCurrency());
        vo.setVerified(profile.getVerified());
        return vo;
    }
}
