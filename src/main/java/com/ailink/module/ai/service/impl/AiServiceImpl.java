package com.ailink.module.ai.service.impl;

import com.ailink.module.ai.client.OpenAiClient;
import com.ailink.module.ai.dto.AiDemandStructRequest;
import com.ailink.module.ai.service.AiService;
import com.ailink.module.ai.vo.AiDemandStructVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AiServiceImpl implements AiService {

    private final OpenAiClient openAiClient;

    public AiServiceImpl(OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    @Override
    public AiDemandStructVO structureDemand(AiDemandStructRequest request) {
        StringBuilder raw = new StringBuilder();
        if (StringUtils.hasText(request.getTargetCountry())) {
            raw.append("targetCountry=").append(request.getTargetCountry()).append("; ");
        }
        if (StringUtils.hasText(request.getCategory())) {
            raw.append("category=").append(request.getCategory()).append("; ");
        }
        raw.append("description=").append(request.getDescription());

        String structured = openAiClient.structureDemand(raw.toString());

        List<String> riskTips = new ArrayList<>();
        String desc = request.getDescription().toLowerCase(Locale.ROOT);
        if (desc.contains("cash") || desc.contains("private transfer")) {
            riskTips.add("Avoid offline payment and use platform escrow only.");
        }
        if (desc.contains("urgent") || desc.contains("today")) {
            riskTips.add("Urgent orders require tighter milestone and acceptance criteria.");
        }
        if (desc.contains("no contract")) {
            riskTips.add("Define digital contract terms before starting work.");
        }
        if (riskTips.isEmpty()) {
            riskTips.add("Use milestone-based delivery and keep evidence in platform chat.");
        }

        List<String> nextSteps = List.of(
                "Publish demand and confirm budget/deadline.",
                "Review top matched verified workers.",
                "Create escrow order before execution.");

        return new AiDemandStructVO(structured, riskTips, nextSteps);
    }
}
