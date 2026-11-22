package com.iitsm.domain.observer;

import java.time.LocalDateTime;

/**
 * Event fired when an incident is classified into a category and service.
 */
public record IncidentClassifiedEvent(
        Long incidentId,
        String incidentDisplayId,
        String category,
        String service,
        LocalDateTime timestamp
) implements IncidentEvent {

    @Override
    public String eventType() {
        return "INCIDENT_CLASSIFIED";
    }
}
