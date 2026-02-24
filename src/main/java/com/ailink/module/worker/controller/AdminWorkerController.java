package com.ailink.module.worker.controller;

import com.ailink.common.Result;
import com.ailink.module.worker.dto.WorkerVerifyRequest;
import com.ailink.module.worker.service.WorkerProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/worker")
public class AdminWorkerController {

    private final WorkerProfileService workerProfileService;

    public AdminWorkerController(WorkerProfileService workerProfileService) {
        this.workerProfileService = workerProfileService;
    }

    @PostMapping("/{profileId}/verify")
    public Result<Void> verifyWorker(@PathVariable Long profileId, @Valid @RequestBody WorkerVerifyRequest request) {
        workerProfileService.verifyByAdmin(profileId, request.getVerified());
        return Result.success();
    }
}
