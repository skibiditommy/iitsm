package com.iitsm.dto.response;

import java.time.LocalDateTime;

public record IncidentSummaryResponse(
        Long id, String incidentId, String title, String calculatedPriority,
        String status, String category, String assignedTechnicianName,
        String responseSlaStatus, String resolutionSlaStatus,
        LocalDateTime createdAt
) {}
