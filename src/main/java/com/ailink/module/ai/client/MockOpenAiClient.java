package com.ailink.module.ai.client;

import org.springframework.stereotype.Component;

@Component
public class MockOpenAiClient implements OpenAiClient {

    @Override
    public String structureDemand(String rawText) {
        String safe = rawText == null ? "" : rawText.replace("\"", "\\\"");
        return "{\"summary\":\"" + safe + "\",\"source\":\"mock-ai-client\"}";
    }
}
