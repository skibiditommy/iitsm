package com.iitsm.infrastructure.adapter;

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
import java.util.*;

/**
 * Groq API Adapter: Cloud LLM inference adapter powered by Groq LPU™
 * and open-weight models (e.g. OpenAI GPT-OSS-120B / Qwen 2.5).
 * Implements the Adapter Pattern for {@link AiAssistantPort} with seamless
 * fallback to {@link LocalNlpAdapter}.
 */
@Slf4j
@Component
@Primary
public class GroqApiAdapter implements AiAssistantPort {

    private final LocalNlpAdapter fallback;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.api.model:openai/gpt-oss-120b}")
    private String model;

    public GroqApiAdapter(LocalNlpAdapter fallback, ObjectMapper objectMapper) {
        this.fallback = fallback;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        log.info("GroqApiAdapter initialized - Groq LPU LLM active with fallback to LocalNlpAdapter");
    }

    @Override
    public CategoryPrediction predictCategory(String title, String description) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallback.predictCategory(title, description);
        }

        try {
            log.info("GroqApiAdapter: classifying incident category via Groq API...");
            String prompt = String.format(
                    "Classify this IT incident into exactly one of: Network, Hardware, Software, Security, Database, Email, Cloud, Authentication, Performance.\nTitle: %s\nDescription: %s\nRespond with ONLY the category name.",
                    title, description != null ? description : ""
            );

            Map<String, Object> payload = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are an IT incident classifier. Answer with just the single category name."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.1,
                    "max_tokens", 20
            );

            String responseText = callGroq(payload);
            if (responseText != null && !responseText.isBlank()) {
                String cleanCat = responseText.trim().replaceAll("[^a-zA-Z]", "");
                if (!cleanCat.isEmpty()) {
                    return new CategoryPrediction(cleanCat, 0.95);
                }
            }
        } catch (Exception e) {
            log.warn("GroqApiAdapter category prediction failed ({}), falling back to LocalNlpAdapter", e.getMessage());
        }

        return fallback.predictCategory(title, description);
    }

    @Override
    public List<SuggestedResolution> suggestResolutions(String title, String description, String category) {
        // Use local incident repository match for real historical incident correlation
        return fallback.suggestResolutions(title, description, category);
    }

    @Override
    public ChatbotProcessingResult processChat(String message, String sessionId) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Groq API key missing, falling back to LocalNlpAdapter");
            return fallback.processChat(message, sessionId);
        }

        try {
            log.info("GroqApiAdapter: generating chatbot response using model {}", model);

            String systemPrompt = """
                    You are Nexora I-ITSM AI Assistant, an expert enterprise IT service desk specialist.
                    Your goal is to assist employees with IT service management, incident diagnosis, self-service troubleshooting, and incident lifecycle guidance.

                    Core Responsibilities:
                    1. Help troubleshoot IT issues (network connectivity, SSO/passwords, software crashes, email delivery, server latency).
                    2. Guide users on how to report an incident with appropriate severity (P1: Critical/Outage, P2: High, P3: Medium, P4: Low).
                    3. Explain incident statuses (NEW, CLASSIFIED, ASSIGNED, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, VERIFIED, CLOSED).
                    4. Keep answers professional, concise, structured, and under 250 words unless detailed diagnostics are required.
                    """;

            Map<String, Object> payload = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", message)
                    ),
                    "temperature", 0.6,
                    "max_tokens", 750
            );

            String content = callGroq(payload);
            if (content != null && !content.isBlank()) {
                List<String> actions = List.of("Create Incident", "View My Tickets", "Check SLA Status");
                return new ChatbotProcessingResult(content.trim(), actions);
            }
        } catch (Exception e) {
            log.error("Groq chat completion call failed: {}, falling back to LocalNlpAdapter", e.getMessage());
        }

        return fallback.processChat(message, sessionId);
    }

    private String callGroq(Map<String, Object> payload) throws Exception {
        String requestJson = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(12))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
        } else {
            log.warn("Groq API error status {}: {}", response.statusCode(), response.body());
        }
        return null;
    }
}
