package com.ailink.module.ai.client;

public interface OpenAiClient {

    String structureDemand(String rawText);

    String generateSmartMilestones(String structuredDemand);

    String generateDisputeArbitrationReport(String context);
}
