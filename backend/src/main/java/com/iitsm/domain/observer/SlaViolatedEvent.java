package com.iitsm.domain.observer;

import java.time.LocalDateTime;

/**
 * Event fired when an SLA deadline has been violated.
 */
public record SlaViolatedEvent(
        Long incidentId,
        String incidentDisplayId,
        String slaType,
        long minutesOverdue,
        LocalDateTime timestamp
) implements IncidentEvent {

    @Override
    public String eventType() {
        return "SLA_VIOLATED";
    }
}
