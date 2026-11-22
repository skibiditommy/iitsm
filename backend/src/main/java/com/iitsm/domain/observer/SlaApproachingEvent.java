package com.iitsm.domain.observer;

import java.time.LocalDateTime;

/**
 * Event fired when an SLA deadline is approaching.
 */
public record SlaApproachingEvent(
        Long incidentId,
        String incidentDisplayId,
        String slaType,
        long minutesRemaining,
        LocalDateTime timestamp
) implements IncidentEvent {

    @Override
    public String eventType() {
        return "SLA_APPROACHING";
    }
}
