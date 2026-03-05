package com.ailink.module.ai.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AiMatchResultVO {
    private Long workerProfileId;
    private Long workerUserId;
    private String workerName;
    private String country;
    private String skillTags;
    private Float rating;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private BigDecimal ruleMatchScore;
    private String aiRecommendReason;
    private Integer aiRank;
    private List<String> matchedKeywords;
}
