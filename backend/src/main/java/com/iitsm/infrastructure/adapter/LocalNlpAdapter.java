package com.iitsm.infrastructure.adapter;

import com.iitsm.domain.model.Incident;
import com.iitsm.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Local NLP Adapter: Rule-based keyword extraction for categorization,
 * Jaccard/TF-IDF similarity for suggested resolutions, and state-machine chatbot.
 * Serves as the @Primary fallback when Gemini API is not configured.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalNlpAdapter implements AiAssistantPort {

    private final IncidentRepository incidentRepository;

    // Category keywords mapping
    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.of(
            "Network", List.of("network", "wifi", "internet", "connectivity", "dns", "vpn", "firewall", "router", "switch", "latency"),
            "Hardware", List.of("hardware", "printer", "monitor", "keyboard", "mouse", "laptop", "desktop", "disk", "memory", "ram"),
            "Software", List.of("software", "application", "app", "crash", "bug", "error", "install", "update", "license", "version"),
            "Security", List.of("security", "password", "access", "unauthorized", "breach", "malware", "virus", "phishing", "hack", "firewall"),
            "Database", List.of("database", "sql", "query", "table", "connection", "timeout", "deadlock", "replication", "backup"),
            "Email", List.of("email", "outlook", "mail", "smtp", "imap", "calendar", "exchange", "inbox"),
            "Cloud", List.of("cloud", "aws", "azure", "gcp", "kubernetes", "docker", "container", "deployment", "serverless"),
            "Authentication", List.of("login", "authentication", "sso", "ldap", "oauth", "token", "session", "password", "mfa"),
            "Performance", List.of("slow", "performance", "speed", "lag", "timeout", "load", "cpu", "memory", "bottleneck")
    );

    @Override
    public CategoryPrediction predictCategory(String title, String description) {
        String combined = (title + " " + (description != null ? description : "")).toLowerCase();
        String bestCategory = "Software"; // default
        int bestScore = 0;

        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (combined.contains(keyword)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestCategory = entry.getKey();
            }
        }

        double confidence = bestScore > 0 ? Math.min(bestScore * 0.2, 0.95) : 0.3;
        log.debug("LocalNlp: predicted category '{}' with confidence {} for title '{}'",
                bestCategory, confidence, title);
        return new CategoryPrediction(bestCategory, confidence);
    }

    @Override
    public List<SuggestedResolution> suggestResolutions(String title, String description, String category) {
        // Find resolved incidents in the same category
        List<Incident> resolved = incidentRepository.findAll().stream()
                .filter(i -> i.getStatus().name().equals("CLOSED") && i.getResolution() != null)
                .collect(Collectors.toList());

        if (resolved.isEmpty()) {
            return List.of();
        }

        String query = (title + " " + (description != null ? description : "")).toLowerCase();
        Set<String> queryTokens = tokenize(query);

        List<SuggestedResolution> suggestions = new ArrayList<>();
        for (Incident inc : resolved) {
            String incText = (inc.getTitle() + " " + (inc.getDescription() != null ? inc.getDescription() : "")).toLowerCase();
            Set<String> incTokens = tokenize(incText);
            double similarity = jaccardSimilarity(queryTokens, incTokens);

            if (similarity > 0.1) {
                suggestions.add(new SuggestedResolution(
                        inc.getIncidentId(), inc.getTitle(),
                        inc.getResolution(), inc.getRootCause(), similarity
                ));
            }
        }

        suggestions.sort(Comparator.comparingDouble(SuggestedResolution::similarityScore).reversed());
        return suggestions.stream().limit(5).collect(Collectors.toList());
    }

    @Override
    public ChatbotProcessingResult processChat(String message, String sessionId) {
        String lower = message.toLowerCase().trim();

        if (lower.contains("create") || lower.contains("report") || lower.contains("new incident")) {
            return new ChatbotProcessingResult(
                    "I can help you create a new incident! Please provide:\n" +
                    "1. A short title for the issue\n" +
                    "2. A detailed description\n" +
                    "3. The severity (Critical/High/Medium/Low)\n\n" +
                    "Or use the incident creation form for a guided experience.",
                    List.of("Open Create Incident Form", "Show my incidents")
            );
        } else if (lower.contains("status") || lower.contains("check") || lower.contains("my incident")) {
            return new ChatbotProcessingResult(
                    "To check the status of your incidents, go to 'My Incidents' in the dashboard. " +
                    "You can also search by incident ID (e.g., INC-1001).",
                    List.of("View My Incidents", "Search by ID")
            );
        } else if (lower.contains("escalate") || lower.contains("urgent")) {
            return new ChatbotProcessingResult(
                    "If your incident needs urgent attention, you can request escalation " +
                    "from your incident details page. An administrator will be notified.",
                    List.of("View Active Incidents", "Contact Support")
            );
        } else if (lower.contains("help") || lower.contains("what can you do")) {
            return new ChatbotProcessingResult(
                    "I'm your IT Service assistant! I can help you with:\n" +
                    "• Creating new incidents\n" +
                    "• Checking incident status\n" +
                    "• Escalating urgent issues\n" +
                    "• Finding solutions from past incidents\n" +
                    "• General IT support questions",
                    List.of("Create Incident", "Check Status", "View Knowledge Base")
            );
        } else if (lower.contains("password") || lower.contains("reset")) {
            return new ChatbotProcessingResult(
                    "For password reset issues, please create a new incident with category 'Authentication'. " +
                    "Common fixes: Clear browser cache, try incognito mode, or use the 'Forgot Password' link.",
                    List.of("Create Authentication Incident", "View FAQ")
            );
        } else {
            return new ChatbotProcessingResult(
                    "I understand you need help with: \"" + message + "\". " +
                    "I'd recommend creating an incident so our team can assist you properly. " +
                    "Would you like me to help you with that?",
                    List.of("Create Incident", "Browse FAQs", "Talk to Agent")
            );
        }
    }

    // ── Utility Methods ──

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.split("\\W+"))
                .filter(s -> s.length() > 2)
                .collect(Collectors.toSet());
    }

    private double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
}
