package com.ailink.module.demand.controller;

import com.ailink.common.Result;
import com.ailink.module.demand.dto.DemandApplyActionRequest;
import com.ailink.module.demand.dto.DemandApplySubmitRequest;
import com.ailink.module.demand.service.DemandApplyService;
import com.ailink.module.demand.vo.DemandApplyVO;
import com.ailink.security.SecurityContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demand")
public class DemandApplyController {

    private final DemandApplyService demandApplyService;

    public DemandApplyController(DemandApplyService demandApplyService) {
        this.demandApplyService = demandApplyService;
    }

    @PostMapping("/{demandId}/apply")
    public Result<DemandApplyVO> submit(@PathVariable Long demandId,
                                        @Valid @RequestBody DemandApplySubmitRequest request) {
        return Result.success(demandApplyService.submit(SecurityContextUtil.currentUserId(), demandId, request));
    }

    @GetMapping("/my/applications")
    public Result<List<DemandApplyVO>> myApplications() {
        return Result.success(demandApplyService.listMyApplications(SecurityContextUtil.currentUserId()));
    }

    @GetMapping("/{demandId}/applications")
    public Result<List<DemandApplyVO>> listByDemandOwner(@PathVariable Long demandId,
                                                         @RequestParam(required = false) String status) {
        return Result.success(demandApplyService.listByDemandOwner(SecurityContextUtil.currentUserId(), demandId, status));
    }

    @PostMapping("/applications/{applyId}/accept")
    public Result<Map<String, Long>> accept(@PathVariable Long applyId,
                                            @RequestBody(required = false) DemandApplyActionRequest request) {
        Long orderId = demandApplyService.acceptByDemandOwner(
                SecurityContextUtil.currentUserId(),
                applyId,
                request == null ? null : request.getAmount(),
                request == null ? null : request.getReviewNote());
        return Result.success(Map.of("orderId", orderId));
    }

    @PostMapping("/applications/{applyId}/reject")
    public Result<Void> reject(@PathVariable Long applyId,
                               @RequestBody(required = false) DemandApplyActionRequest request) {
        demandApplyService.rejectByDemandOwner(
                SecurityContextUtil.currentUserId(),
                applyId,
                request == null ? null : request.getReviewNote());
        return Result.success();
    }

    @PostMapping("/applications/{applyId}/cancel")
    public Result<Void> cancel(@PathVariable Long applyId) {
        demandApplyService.cancelByWorker(SecurityContextUtil.currentUserId(), applyId);
        return Result.success();
    }
}

