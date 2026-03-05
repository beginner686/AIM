package com.ailink.module.ai.service;

import com.ailink.module.ai.vo.AiMatchResultVO;

import java.util.List;

public interface AiMatchService {
    /**
     * AI 托管自动匹配：根据需求 id 从执行者池拉取候选者，
     * 然后调用 DeepSeek 做语义匹配并返回推荐结果（附上推荐理由）
     */
    List<AiMatchResultVO> autoMatch(Long demandId);
}
