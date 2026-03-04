package com.ailink.module.ai.client;

public class MockOpenAiClient implements OpenAiClient {

    @Override
    public String structureDemand(String rawText) {
        String safe = rawText == null ? "" : rawText.replace("\"", "\\\"");
        return "{\"summary\":\"" + safe + "\",\"source\":\"mock-ai-client\"}";
    }

    @Override
    public String generateSmartMilestones(String structuredDemand) {
        return "1. Confirm exact deliverables\\n2. Set clear milestones\\n3. Submit work in Order Chat\\n(Mock AI Generated)";
    }

    @Override
    public String generateDisputeArbitrationReport(String context) {
        return "{\\n" +
                "  \"summary\": \"Mock dispute analysis based on chat logs.\",\\n" +
                "  \"faulty_party\": \"UNKNOWN\",\\n" +
                "  \"delivery_matched_score\": 50,\\n" +
                "  \"suggestion\": \"Please review the evidence manually as this is a mock AI response.\",\\n" +
                "  \"source\": \"mock-ai-client\"\\n" +
                "}";
    }
}
