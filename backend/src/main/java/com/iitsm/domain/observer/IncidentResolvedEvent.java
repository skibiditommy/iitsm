package com.iitsm.domain.observer;

import java.time.LocalDateTime;

/**
 * Event fired when an incident is resolved.
 */
public record IncidentResolvedEvent(
        Long incidentId,
        String incidentDisplayId,
        String technicianEmail,
        String resolution,
        LocalDateTime timestamp
) implements IncidentEvent {

    @Override
    public String eventType() {
        return "INCIDENT_RESOLVED";
    }
}
