package com.iitsm.dto.response;

import java.util.List;

public record ChatbotResponse(String reply, List<String> suggestedActions, String sessionId) {}
