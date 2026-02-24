package com.ailink.module.ai.controller;

import com.ailink.common.Result;
import com.ailink.module.ai.dto.AiDemandStructRequest;
import com.ailink.module.ai.service.AiService;
import com.ailink.module.ai.vo.AiDemandStructVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/demand/structure")
    public Result<AiDemandStructVO> structureDemand(@Valid @RequestBody AiDemandStructRequest request) {
        return Result.success(aiService.structureDemand(request));
    }
}
