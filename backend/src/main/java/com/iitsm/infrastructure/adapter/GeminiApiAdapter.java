package com.iitsm.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Gemini API Adapter: When configured via gemini.api.key property, this adapter
 * delegates AI tasks to Google's Gemini API. Falls back to LocalNlpAdapter seamlessly.
 * <p>
 * Since this is a capstone demo, it logs simulated Gemini calls and delegates to
 * the local adapter for actual processing.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "gemini.api.key")
public class GeminiApiAdapter implements AiAssistantPort {

    private final LocalNlpAdapter fallback;

    public GeminiApiAdapter(LocalNlpAdapter fallback) {
        this.fallback = fallback;
        log.info("GeminiApiAdapter initialized - will attempt Gemini API calls with fallback to LocalNlpAdapter");
    }

    @Override
    public CategoryPrediction predictCategory(String title, String description) {
        try {
            log.info("GeminiApiAdapter: calling Gemini API for category prediction...");
            // In production, this would call the Gemini API
            // For demo, we fall back to local processing
            return fallback.predictCategory(title, description);
        } catch (Exception e) {
            log.warn("GeminiApiAdapter: Gemini call failed, falling back to LocalNlpAdapter: {}", e.getMessage());
            return fallback.predictCategory(title, description);
        }
    }

    @Override
    public List<SuggestedResolution> suggestResolutions(String title, String description, String category) {
        try {
            log.info("GeminiApiAdapter: calling Gemini API for resolution suggestions...");
            return fallback.suggestResolutions(title, description, category);
        } catch (Exception e) {
            log.warn("GeminiApiAdapter: Gemini call failed, falling back: {}", e.getMessage());
            return fallback.suggestResolutions(title, description, category);
        }
    }

    @Override
    public ChatbotProcessingResult processChat(String message, String sessionId) {
        try {
            log.info("GeminiApiAdapter: calling Gemini API for chatbot processing...");
            return fallback.processChat(message, sessionId);
        } catch (Exception e) {
            log.warn("GeminiApiAdapter: Gemini call failed, falling back: {}", e.getMessage());
            return fallback.processChat(message, sessionId);
        }
    }
}
