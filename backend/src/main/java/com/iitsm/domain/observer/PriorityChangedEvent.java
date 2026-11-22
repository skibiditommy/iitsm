package com.iitsm.domain.observer;

import java.time.LocalDateTime;

/**
 * Event fired when an incident's priority is recalculated or changed.
 */
public record PriorityChangedEvent(
        Long incidentId,
        String incidentDisplayId,
        String oldPriority,
        String newPriority,
        double score,
        LocalDateTime timestamp
) implements IncidentEvent {

    @Override
    public String eventType() {
        return "PRIORITY_CHANGED";
    }
}
