package com.iitsm.dto.response;

import java.util.List;

public record RootCauseSuggestionResponse(
        Long serviceId,
        String serviceName,
        int businessCriticality,
        int blastRadiusCount,
        int affectedDownstreamServicesCount,
        List<String> affectedDownstreamServiceNames,
        List<String> activeIncidentIds,
        double confidenceScore,
        String recommendation
) {}
