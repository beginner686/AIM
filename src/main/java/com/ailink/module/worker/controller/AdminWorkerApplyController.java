package com.ailink.module.worker.controller;

import com.ailink.common.Result;
import com.ailink.module.worker.dto.WorkerApplyQueryRequest;
import com.ailink.module.worker.dto.WorkerApplyReviewRequest;
import com.ailink.module.worker.service.WorkerApplyService;
import com.ailink.module.worker.vo.WorkerApplyVO;
import com.ailink.security.SecurityContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/worker/apply")
public class AdminWorkerApplyController {

    private final WorkerApplyService workerApplyService;

    public AdminWorkerApplyController(WorkerApplyService workerApplyService) {
        this.workerApplyService = workerApplyService;
    }

    @GetMapping("/list")
    public Result<List<WorkerApplyVO>> list(WorkerApplyQueryRequest request) {
        return Result.success(workerApplyService.listByAdmin(request));
    }

    @PostMapping("/{applyId}/approve")
    public Result<Void> approve(@PathVariable Long applyId,
                                @RequestBody(required = false) @Valid WorkerApplyReviewRequest request) {
        workerApplyService.approve(
                SecurityContextUtil.currentUserId(),
                applyId,
                request == null ? null : request.getReviewNote());
        return Result.success();
    }

    @PostMapping("/{applyId}/reject")
    public Result<Void> reject(@PathVariable Long applyId,
                               @RequestBody(required = false) @Valid WorkerApplyReviewRequest request) {
        workerApplyService.reject(
                SecurityContextUtil.currentUserId(),
                applyId,
                request == null ? null : request.getReviewNote());
        return Result.success();
    }
}
