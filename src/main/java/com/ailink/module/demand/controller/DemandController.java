package com.ailink.module.demand.controller;

import com.ailink.common.Result;
import com.ailink.module.demand.dto.DemandCreateRequest;
import com.ailink.module.demand.dto.DemandQueryRequest;
import com.ailink.module.demand.service.DemandService;
import com.ailink.module.demand.vo.DemandVO;
import com.ailink.security.SecurityContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demand")
public class DemandController {

    private final DemandService demandService;

    public DemandController(DemandService demandService) {
        this.demandService = demandService;
    }

    @PostMapping
    public Result<Map<String, Long>> createDemand(@Valid @RequestBody DemandCreateRequest request) {
        Long demandId = demandService.createDemand(SecurityContextUtil.currentUserId(), request);
        return Result.success(Map.of("demandId", demandId));
    }

    @GetMapping
    public Result<List<DemandVO>> listDemand(DemandQueryRequest request) {
        return Result.success(demandService.listDemand(request));
    }

    @GetMapping("/my")
    public Result<List<DemandVO>> myDemand() {
        return Result.success(demandService.listMyDemand(SecurityContextUtil.currentUserId()));
    }

    @GetMapping("/{demandId}")
    public Result<DemandVO> detail(@PathVariable Long demandId) {
        return Result.success(demandService.getById(demandId));
    }
}
