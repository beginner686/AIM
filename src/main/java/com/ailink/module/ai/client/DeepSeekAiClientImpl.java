package com.ailink.module.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Primary
@Component
public class DeepSeekAiClientImpl implements OpenAiClient {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DeepSeekAiClientImpl(@Value("${ai.deepseek.api-key}") String apiKey,
            @Value("${ai.deepseek.base-url:https://api.deepseek.com/chat/completions}") String baseUrl,
            @Value("${ai.deepseek.model:deepseek-chat}") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public String structureDemand(String rawText) {
        String prompt = "You are a demand structuring assistant. Extract the core requirements from the user's raw text and format it into a clear, structured JSON containing: "
                +
                "1. Core deliverable\\n2. Important constraints.\\nJust output JSON without any markdown formatting wrappers.\\n\\nRaw Text: "
                + rawText;
        return callDeepSeek(prompt, true);
    }

    @Override
    public String generateSmartMilestones(String structuredDemand) {
        String prompt = "You are an AI Escrow Contract Guard. Based on the following structured demand, generate 3-5 clear milestone nodes for the worker to follow and the employer to verify. "
                +
                "Output as a numbered list with actionable items. Keep it concise.\\n\\nDemand Context:\\n"
                + structuredDemand;
        return callDeepSeek(prompt, false);
    }

    @Override
    public String generateDisputeArbitrationReport(String context) {
        String prompt = "You are an impartial AI Arbitration Judge for a digital labor platform. " +
                "Review the order info, demand target, and chat evidence. Determine the responsible party (EMPLOYER or WORKER) for the dispute, assign a delivery matched score (0-100), and summarize the case.\\n"
                +
                "Return pure JSON format EXACTLY like this (NO markdown blocks, NO triple backticks):\\n" +
                "{\\n" +
                "  \\\"summary\\\": \\\"Brief explanation of the dispute...\\\",\\n" +
                "  \\\"faulty_party\\\": \\\"EMPLOYER or WORKER or UNKNOWN\\\",\\n" +
                "  \\\"delivery_matched_score\\\": 75,\\n" +
                "  \\\"suggestion\\\": \\\"Your recommended resolution...\\\"\\n" +
                "}\\n\\nEvidences Context:\\n" + context;
        String response = callDeepSeek(prompt, true);
        if (response.startsWith("```json")) {
            response = response.substring(7);
            if (response.endsWith("```")) {
                response = response.substring(0, response.length() - 3);
            }
        }
        return response.trim();
    }

    private String callDeepSeek(String userMessage, boolean retryOnFail) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "user", "content", userMessage)),
                    "temperature", 0.1,
                    "stream", false);

            String jsonPayload = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    return choices.get(0).path("message").path("content").asText();
                }
            } else {
                log.error("DeepSeek API error: {} - {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to call DeepSeek API", e);
        }
        return retryOnFail ? "{\"error\": \"Failed to fetch AI analysis\"}"
                : "AI Analysis Unavailable due to API error.";
    }
}
