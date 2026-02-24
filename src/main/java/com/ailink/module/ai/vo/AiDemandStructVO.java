package com.ailink.module.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AiDemandStructVO {

    private String structuredJson;
    private List<String> riskTips;
    private List<String> suggestedNextSteps;
}
