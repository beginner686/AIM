package com.ailink.module.ai.service.impl;

import com.ailink.module.ai.client.OpenAiClient;
import com.ailink.module.ai.service.AiMatchService;
import com.ailink.module.ai.vo.AiMatchResultVO;
import com.ailink.module.demand.entity.Demand;
import com.ailink.module.demand.mapper.DemandMapper;
import com.ailink.module.match.service.MatchService;
import com.ailink.module.match.vo.MatchWorkerVO;
import com.ailink.module.user.entity.User;
import com.ailink.module.user.mapper.UserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiMatchServiceImpl implements AiMatchService {

    private final MatchService matchService;
    private final DemandMapper demandMapper;
    private final UserMapper userMapper;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public AiMatchServiceImpl(MatchService matchService,
            DemandMapper demandMapper,
            UserMapper userMapper,
            OpenAiClient openAiClient,
            ObjectMapper objectMapper) {
        this.matchService = matchService;
        this.demandMapper = demandMapper;
        this.userMapper = userMapper;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AiMatchResultVO> autoMatch(Long demandId) {
        Demand demand = demandMapper.selectById(demandId);
        if (demand == null) {
            return List.of();
        }

        // 1. 规则匹配拿出 top 10 候选者
        List<MatchWorkerVO> candidates = matchService.matchWorkers(demandId);
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<MatchWorkerVO> top10 = candidates.stream().limit(10).toList();

        // 2. 构建发送给 AI 的上下文
        StringBuilder workerList = new StringBuilder();
        for (int i = 0; i < top10.size(); i++) {
            MatchWorkerVO w = top10.get(i);
            workerList.append("ID:").append(w.getWorkerProfileId())
                    .append(",国家:").append(w.getCountry())
                    .append(",技能:").append(w.getSkillTags())
                    .append(",评分:").append(w.getRating())
                    .append(",报价区间:").append(w.getPriceMin()).append("-").append(w.getPriceMax())
                    .append(",规则匹配分:").append(w.getMatchScore())
                    .append("\n");
        }

        String demandDesc = String.format(
                "需求类别:%s\n目标国家:%s\n预算:%s\n描述:%s\nAI结构化信息:%s",
                demand.getCategory(), demand.getTargetCountry(),
                demand.getBudget(), demand.getDescription(),
                demand.getAiStructured() == null ? "无" : demand.getAiStructured());

        String prompt = "你是一位顶级跨境人才匹配专家。请根据以下需求，从执行者候选列表中挑选出最适合的前3名，" +
                "输出纯 JSON 数组（不要 markdown 包裹），每个元素格式如下：\n" +
                "{\"workerProfileId\": 数字ID, \"aiRank\": 排名1到3, \"aiRecommendReason\": \"推荐理由（中文，50字以内）\", " +
                "\"matchedKeywords\": [\"关键词1\",\"关键词2\"]}\n\n" +
                "【需求信息】\n" + demandDesc + "\n\n" +
                "【候选执行者列表】\n" + workerList;

        // 3. 调用 AI 获取语义匹配结果
        List<Long> aiRankedIds = new ArrayList<>();
        Map<Long, String> aiReasonMap = new java.util.HashMap<>();
        Map<Long, Integer> aiRankMap = new java.util.HashMap<>();
        Map<Long, List<String>> aiKeywordsMap = new java.util.HashMap<>();

        try {
            String aiJson = openAiClient.structureDemand(prompt); // 复用 structureDemand 作为通用 text 请求
            // 解析 AI 返回的 JSON 数组
            if (aiJson.startsWith("```")) {
                aiJson = aiJson.replaceAll("```json", "").replaceAll("```", "").trim();
            }
            List<Map<String, Object>> aiResult = objectMapper.readValue(aiJson, new TypeReference<>() {
            });
            for (Map<String, Object> item : aiResult) {
                Long id = Long.valueOf(item.get("workerProfileId").toString());
                int rank = Integer.parseInt(item.get("aiRank").toString());
                String reason = item.getOrDefault("aiRecommendReason", "").toString();
                @SuppressWarnings("unchecked")
                List<String> keywords = (List<String>) item.getOrDefault("matchedKeywords", List.of());
                aiRankedIds.add(id);
                aiReasonMap.put(id, reason);
                aiRankMap.put(id, rank);
                aiKeywordsMap.put(id, keywords);
            }
        } catch (Exception e) {
            log.warn("AI match parsing failed, fallback to rule-based top3, error: {}", e.getMessage());
            // fallback: 直接取规则前3
            for (int i = 0; i < Math.min(3, top10.size()); i++) {
                MatchWorkerVO w = top10.get(i);
                aiRankedIds.add(w.getWorkerProfileId());
                aiReasonMap.put(w.getWorkerProfileId(), "规则评分匹配，综合技能与国家相符");
                aiRankMap.put(w.getWorkerProfileId(), i + 1);
                aiKeywordsMap.put(w.getWorkerProfileId(), List.of());
            }
        }

        // 4. 组装返回 VO
        Map<Long, MatchWorkerVO> candidateMap = top10.stream()
                .collect(Collectors.toMap(MatchWorkerVO::getWorkerProfileId, v -> v));

        return aiRankedIds.stream()
                .filter(candidateMap::containsKey)
                .map(id -> {
                    MatchWorkerVO w = candidateMap.get(id);
                    AiMatchResultVO vo = new AiMatchResultVO();
                    vo.setWorkerProfileId(w.getWorkerProfileId());
                    vo.setWorkerUserId(w.getWorkerUserId());
                    vo.setCountry(w.getCountry());
                    vo.setSkillTags(w.getSkillTags());
                    vo.setRating(w.getRating());
                    vo.setPriceMin(w.getPriceMin());
                    vo.setPriceMax(w.getPriceMax());
                    vo.setRuleMatchScore(w.getMatchScore());
                    vo.setAiRecommendReason(aiReasonMap.getOrDefault(id, ""));
                    vo.setAiRank(aiRankMap.getOrDefault(id, 0));
                    vo.setMatchedKeywords(aiKeywordsMap.getOrDefault(id, List.of()));
                    // 查询执行者昵称
                    try {
                        User user = userMapper.selectById(w.getWorkerUserId());
                        vo.setWorkerName(user != null ? user.getUsername() : "执行者#" + id);
                    } catch (Exception ignore) {
                        vo.setWorkerName("执行者#" + id);
                    }
                    return vo;
                })
                .sorted(java.util.Comparator.comparingInt(AiMatchResultVO::getAiRank))
                .toList();
    }
}
