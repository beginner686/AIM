package com.ailink.module.order.controller;

import com.ailink.common.Result;
import com.ailink.module.order.dto.FeeConfigUpdateRequest;
import com.ailink.module.order.service.PlatformConfigService;
import com.ailink.module.order.vo.FeeConfigVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/config")
public class AdminConfigController {

    private final PlatformConfigService platformConfigService;

    public AdminConfigController(PlatformConfigService platformConfigService) {
        this.platformConfigService = platformConfigService;
    }

    @GetMapping("/fees")
    public Result<FeeConfigVO> getFeeConfig() {
        return Result.success(platformConfigService.getFeeConfig());
    }

    @PostMapping("/fees")
    public Result<Void> updateFeeConfig(@Valid @RequestBody FeeConfigUpdateRequest request) {
        platformConfigService.updateFeeConfig(request.getPlatformFeeRate(), request.getEscrowFeeRate());
        return Result.success();
    }
}
