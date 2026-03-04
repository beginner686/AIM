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

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class RunnerPaymentProfileServiceImpl implements RunnerPaymentProfileService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SIMPLE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{4,64}$");
    private static final Pattern CRYPTO_WALLET_PATTERN = Pattern.compile("^[A-Za-z0-9:_-]{12,128}$");
    private static final Set<String> PAYPAL_ALLOWED_DOMAINS = Set.of("paypal.me", "paypal.com");
    private static final Set<String> WISE_ALLOWED_DOMAINS = Set.of("wise.com");
    private static final Set<String> PAYONEER_ALLOWED_DOMAINS = Set.of("payoneer.com");

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
        String paypalEmail = trimToNull(request.getPaypalEmail());
        String wiseId = trimToNull(request.getWiseId());
        String wiseLink = trimToNull(request.getWiseLink());
        String payoneerLink = trimToNull(request.getPayoneerLink());
        String cryptoWallet = trimToNull(request.getCryptoWallet());
        String paymentUrl = trimToNull(request.getPaymentUrl());

        validatePaymentReceivers(paypalEmail, wiseId, wiseLink, payoneerLink, cryptoWallet, paymentUrl);

        profile.setPaypalEmail(paypalEmail);
        profile.setWiseId(wiseId);
        profile.setWiseLink(wiseLink);
        profile.setPayoneerLink(payoneerLink);
        profile.setCryptoWallet(cryptoWallet);
        profile.setPaymentUrl(paymentUrl);
        profile.setCurrency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency().trim().toUpperCase() : "CNY");
    }

    private void validatePaymentReceivers(String paypalEmail,
                                          String wiseId,
                                          String wiseLink,
                                          String payoneerLink,
                                          String cryptoWallet,
                                          String paymentUrl) {
        if (StringUtils.hasText(paypalEmail) && !EMAIL_PATTERN.matcher(paypalEmail).matches()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "invalid paypal email");
        }
        if (StringUtils.hasText(wiseId) && !SIMPLE_ID_PATTERN.matcher(wiseId).matches()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "invalid wise id");
        }
        if (StringUtils.hasText(wiseLink)) {
            validateAllowedUrl(wiseLink, WISE_ALLOWED_DOMAINS, "invalid wise link");
        }
        if (StringUtils.hasText(payoneerLink)) {
            validateAllowedUrl(payoneerLink, PAYONEER_ALLOWED_DOMAINS, "invalid payoneer link");
        }
        if (StringUtils.hasText(paymentUrl)) {
            validateAllowedUrl(paymentUrl, PAYPAL_ALLOWED_DOMAINS, "invalid payment url");
        }
        if (StringUtils.hasText(cryptoWallet) && !CRYPTO_WALLET_PATTERN.matcher(cryptoWallet).matches()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "invalid crypto wallet");
        }
    }

    private void validateAllowedUrl(String rawUrl, Set<String> allowedDomains, String errorMessage) {
        try {
            URI uri = URI.create(rawUrl.trim());
            String scheme = String.valueOf(uri.getScheme()).toLowerCase(Locale.ROOT);
            String host = String.valueOf(uri.getHost()).toLowerCase(Locale.ROOT);
            if (!"https".equals(scheme)) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(), errorMessage);
            }
            boolean matched = allowedDomains.stream().anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
            if (!matched) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(), errorMessage);
            }
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), errorMessage);
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private RunnerPaymentProfileVO toVO(RunnerPaymentProfile profile) {
        RunnerPaymentProfileVO vo = new RunnerPaymentProfileVO();
        vo.setId(profile.getId());
        vo.setUserId(profile.getUserId());
        vo.setPaypalEmail(profile.getPaypalEmail());
        vo.setWiseId(profile.getWiseId());
        vo.setWiseLink(profile.getWiseLink());
        vo.setPayoneerLink(profile.getPayoneerLink());
        vo.setCryptoWallet(profile.getCryptoWallet());
        vo.setPaymentUrl(profile.getPaymentUrl());
        vo.setCurrency(profile.getCurrency());
        vo.setVerified(profile.getVerified());
        return vo;
    }
}
