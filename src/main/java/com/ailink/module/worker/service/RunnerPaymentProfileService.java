package com.ailink.module.worker.service;

import com.ailink.module.worker.dto.RunnerPaymentProfileUpsertRequest;
import com.ailink.module.worker.vo.RunnerPaymentProfileVO;

public interface RunnerPaymentProfileService {

    RunnerPaymentProfileVO getMyProfile(Long userId);

    RunnerPaymentProfileVO upsertMyProfile(Long userId, String role, RunnerPaymentProfileUpsertRequest request);

    RunnerPaymentProfileVO getByUserId(Long userId);
}
