package com.iitsm.domain.observer;

import java.time.LocalDateTime;

/**
 * Event fired when a new incident is created.
 */
public record IncidentCreatedEvent(
        Long incidentId,
        String incidentDisplayId,
        String title,
        String reporterEmail,
        String priority,
        LocalDateTime timestamp
) implements IncidentEvent {

    @Override
    public String eventType() {
        return "INCIDENT_CREATED";
    }
}
