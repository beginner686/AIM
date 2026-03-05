package com.ailink.module.ai.controller;

import com.ailink.common.Result;
import com.ailink.module.ai.service.AiMatchService;
import com.ailink.module.ai.vo.AiMatchResultVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai-match")
public class AiMatchController {

    private final AiMatchService aiMatchService;

    public AiMatchController(AiMatchService aiMatchService) {
        this.aiMatchService = aiMatchService;
    }

    /**
     * AI 托管自动匹配：根据需求 ID 自动从执行者池中选出最佳候选者
     */
    @PostMapping("/demand/{demandId}/auto")
    public Result<List<AiMatchResultVO>> autoMatch(@PathVariable Long demandId) {
        return Result.success(aiMatchService.autoMatch(demandId));
    }
}
