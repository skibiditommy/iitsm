package com.iitsm.dto.response;

import java.util.Map;

public record AnalyticsOverviewResponse(
        long totalIncidents, long activeIncidents, long resolvedIncidents, long closedIncidents,
        long slaBreachedCount, double avgResolutionMinutes, double avgSatisfactionRating,
        Map<String, Long> incidentsByStatus, Map<String, Long> incidentsByPriority,
        Map<String, Long> incidentsByCategory
) {}
