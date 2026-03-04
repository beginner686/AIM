package com.ailink.module.worker.controller;

import com.ailink.common.Result;
import com.ailink.module.worker.dto.WorkerApplySubmitRequest;
import com.ailink.module.worker.service.WorkerApplyService;
import com.ailink.module.worker.vo.WorkerApplyAttachmentVO;
import com.ailink.module.worker.vo.WorkerApplyVO;
import com.ailink.security.SecurityContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/worker/apply")
public class WorkerApplyController {

    private final WorkerApplyService workerApplyService;

    public WorkerApplyController(WorkerApplyService workerApplyService) {
        this.workerApplyService = workerApplyService;
    }

    @PostMapping
    public Result<WorkerApplyVO> submit(@Valid @RequestBody WorkerApplySubmitRequest request) {
        return Result.success(workerApplyService.submit(
                SecurityContextUtil.currentUserId(),
                SecurityContextUtil.currentRole(),
                request));
    }

    @PostMapping("/attachment")
    public Result<WorkerApplyAttachmentVO> uploadAttachment(@RequestParam("file") MultipartFile file) {
        return Result.success(workerApplyService.uploadAttachment(SecurityContextUtil.currentUserId(), file));
    }

    @GetMapping("/me")
    public Result<WorkerApplyVO> myLatest() {
        return Result.success(workerApplyService.getMyLatest(SecurityContextUtil.currentUserId()));
    }
}
