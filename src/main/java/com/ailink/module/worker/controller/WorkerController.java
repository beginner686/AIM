package com.ailink.module.worker.controller;

import com.ailink.common.Result;
import com.ailink.module.worker.dto.WorkerProfileUpsertRequest;
import com.ailink.module.worker.dto.WorkerQueryRequest;
import com.ailink.module.worker.service.WorkerProfileService;
import com.ailink.module.worker.vo.WorkerProfileVO;
import com.ailink.security.SecurityContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/worker")
public class WorkerController {

    private final WorkerProfileService workerProfileService;

    public WorkerController(WorkerProfileService workerProfileService) {
        this.workerProfileService = workerProfileService;
    }

    @PostMapping("/profile")
    public Result<Map<String, Long>> upsertProfile(@Valid @RequestBody WorkerProfileUpsertRequest request) {
        Long profileId = workerProfileService.upsertProfile(SecurityContextUtil.currentUserId(), request);
        return Result.success(Map.of("profileId", profileId));
    }

    @GetMapping("/profile/me")
    public Result<WorkerProfileVO> myProfile() {
        return Result.success(workerProfileService.getByUserId(SecurityContextUtil.currentUserId()));
    }

    @GetMapping("/list")
    public Result<List<WorkerProfileVO>> list(WorkerQueryRequest request) {
        return Result.success(workerProfileService.list(request));
    }
}
