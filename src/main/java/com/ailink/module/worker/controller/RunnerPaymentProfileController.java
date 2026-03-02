package com.ailink.module.worker.controller;

import com.ailink.common.Result;
import com.ailink.module.worker.dto.RunnerPaymentProfileUpsertRequest;
import com.ailink.module.worker.service.RunnerPaymentProfileService;
import com.ailink.module.worker.vo.RunnerPaymentProfileVO;
import com.ailink.security.SecurityContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runner/payment-profile")
public class RunnerPaymentProfileController {

    private final RunnerPaymentProfileService runnerPaymentProfileService;

    public RunnerPaymentProfileController(RunnerPaymentProfileService runnerPaymentProfileService) {
        this.runnerPaymentProfileService = runnerPaymentProfileService;
    }

    @GetMapping("/me")
    public Result<RunnerPaymentProfileVO> myProfile() {
        return Result.success(runnerPaymentProfileService.getMyProfile(SecurityContextUtil.currentUserId()));
    }

    @PostMapping("/me")
    public Result<RunnerPaymentProfileVO> upsert(@Valid @RequestBody RunnerPaymentProfileUpsertRequest request) {
        return Result.success(runnerPaymentProfileService.upsertMyProfile(
                SecurityContextUtil.currentUserId(),
                SecurityContextUtil.currentRole(),
                request));
    }
}
