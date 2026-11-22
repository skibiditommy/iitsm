package com.iitsm.domain.observer;

import java.time.LocalDateTime;

/**
 * Event fired when an incident is closed.
 */
public record IncidentClosedEvent(
        Long incidentId,
        String incidentDisplayId,
        LocalDateTime timestamp
) implements IncidentEvent {

    @Override
    public String eventType() {
        return "INCIDENT_CLOSED";
    }
}
