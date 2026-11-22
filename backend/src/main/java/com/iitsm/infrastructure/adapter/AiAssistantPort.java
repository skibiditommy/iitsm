package com.iitsm.infrastructure.adapter;

import java.util.List;

/**
 * Port (Adapter Pattern): Abstraction for AI/NLP assistant capabilities.
 * Implementations include a local rule-based adapter and a Gemini API adapter.
 */
public interface AiAssistantPort {

    /**
     * Predicts the best category for an incident based on its title and description.
     */
    CategoryPrediction predictCategory(String title, String description);

    /**
     * Finds similar resolved incidents for suggested resolutions.
     */
    List<SuggestedResolution> suggestResolutions(String title, String description, String category);

    /**
     * Processes a chatbot message and returns a response.
     */
    ChatbotProcessingResult processChat(String message, String sessionId);

    // ── Inner Records ──

    record CategoryPrediction(String categoryName, double confidence) {}

    record ResolvedIncidentSummary(String incidentId, String title, String resolution,
                                    String rootCause, double similarity) {}

    record SuggestedResolution(String incidentId, String title, String resolution,
                                String rootCause, double similarityScore) {}

    record ChatbotProcessingResult(String reply, List<String> suggestedActions) {}
}
