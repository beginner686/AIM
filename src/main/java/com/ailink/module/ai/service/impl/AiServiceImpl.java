package com.ailink.module.ai.service.impl;

import com.ailink.module.ai.client.OpenAiClient;
import com.ailink.module.ai.dto.AiDemandStructRequest;
import com.ailink.module.ai.service.AiService;
import com.ailink.module.ai.vo.AiDemandStructVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ailink.module.demand.entity.Demand;
import com.ailink.module.demand.mapper.DemandMapper;
import com.ailink.module.order.entity.ChatMessage;
import com.ailink.module.order.entity.Order;
import com.ailink.module.order.mapper.ChatMessageMapper;
import com.ailink.module.order.mapper.OrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AiServiceImpl implements AiService {

    private final OpenAiClient openAiClient;
    private final OrderMapper orderMapper;
    private final DemandMapper demandMapper;
    private final ChatMessageMapper chatMessageMapper;

    public AiServiceImpl(OpenAiClient openAiClient, OrderMapper orderMapper,
            DemandMapper demandMapper, ChatMessageMapper chatMessageMapper) {
        this.openAiClient = openAiClient;
        this.orderMapper = orderMapper;
        this.demandMapper = demandMapper;
        this.chatMessageMapper = chatMessageMapper;
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

    @Override
    public String generateSmartMilestones(String structuredDemand) {
        if (!StringUtils.hasText(structuredDemand)) {
            return "Please follow standard milestone guidelines.";
        }
        return openAiClient.generateSmartMilestones(structuredDemand);
    }

    @Override
    public String generateDisputeArbitrationReport(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return "Order not found.";
        }
        Demand demand = demandMapper.selectById(order.getDemandId());

        List<ChatMessage> chatMessages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getOrderId, orderId)
                .orderByAsc(ChatMessage::getCreatedTime));

        StringBuilder context = new StringBuilder();
        context.append("--- Order Info ---\\n");
        context.append("Amount: ").append(order.getAmount()).append("\\n");
        context.append("Employer ID: ").append(order.getEmployerId()).append("\\n");
        context.append("Worker ID: ").append(order.getWorkerUserId()).append("\\n");

        if (demand != null) {
            context.append("\\n--- Demand Target ---\\n");
            context.append("Category: ").append(demand.getCategory()).append("\\n");
            context.append("Structured Goals: ").append(demand.getAiStructured()).append("\\n");
        }

        context.append("\\n--- Chat Evidence ---\\n");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (ChatMessage msg : chatMessages) {
            String timeStr = msg.getCreatedTime() != null ? dtf.format(msg.getCreatedTime()) : "";
            context.append("[").append(timeStr).append("] ")
                    .append("User ").append(msg.getSenderId()).append(": ")
                    .append(msg.getContent()).append("\\n");
        }

        return openAiClient.generateDisputeArbitrationReport(context.toString());
    }
}
