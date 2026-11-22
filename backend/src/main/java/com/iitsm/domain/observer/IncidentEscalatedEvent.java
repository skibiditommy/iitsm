package com.iitsm.domain.observer;

import java.time.LocalDateTime;

/**
 * Event fired when an incident is escalated to a higher level.
 */
public record IncidentEscalatedEvent(
        Long incidentId,
        String incidentDisplayId,
        String previousLevel,
        String newLevel,
        String reason,
        LocalDateTime timestamp
) implements IncidentEvent {

    @Override
    public String eventType() {
        return "INCIDENT_ESCALATED";
    }
}
