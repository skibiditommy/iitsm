package com.iitsm.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record IncidentResponse(
        Long id, String incidentId, String title, String description,
        String category, String affectedService,
        String impact, String urgency, int affectedUsersCount,
        double priorityScore, String priorityExplanation, String calculatedPriority,
        String status, String reporterEmail, String reporterName,
        String assignedTechnicianEmail, String assignedTechnicianName,
        LocalDateTime responseDeadline, LocalDateTime resolutionDeadline,
        String responseSlaStatus, String resolutionSlaStatus,
        LocalDateTime respondedAt, String escalationLevel,
        String rootCause, String resolution, String preventiveMeasures,
        LocalDateTime resolvedAt, String verificationFeedback, Integer satisfactionRating,
        LocalDateTime createdAt, LocalDateTime closedAt,
        List<AuditLogResponse> auditTrail, List<CommentResponse> comments,
        String attachmentName, String attachmentType, String attachmentData,
        int reopenCount
) {
    public IncidentResponse(
            Long id, String incidentId, String title, String description,
            String category, String affectedService,
            String impact, String urgency, int affectedUsersCount,
            double priorityScore, String priorityExplanation, String calculatedPriority,
            String status, String reporterEmail, String reporterName,
            String assignedTechnicianEmail, String assignedTechnicianName,
            LocalDateTime responseDeadline, LocalDateTime resolutionDeadline,
            String responseSlaStatus, String resolutionSlaStatus,
            LocalDateTime respondedAt, String escalationLevel,
            String rootCause, String resolution, String preventiveMeasures,
            LocalDateTime resolvedAt, String verificationFeedback, Integer satisfactionRating,
            LocalDateTime createdAt, LocalDateTime closedAt,
            List<AuditLogResponse> auditTrail, List<CommentResponse> comments
    ) {
        this(id, incidentId, title, description, category, affectedService,
                impact, urgency, affectedUsersCount, priorityScore, priorityExplanation, calculatedPriority,
                status, reporterEmail, reporterName, assignedTechnicianEmail, assignedTechnicianName,
                responseDeadline, resolutionDeadline, responseSlaStatus, resolutionSlaStatus,
                respondedAt, escalationLevel, rootCause, resolution, preventiveMeasures,
                resolvedAt, verificationFeedback, satisfactionRating, createdAt, closedAt,
                auditTrail, comments, null, null, null, 0);
    }
}
