package com.iitsm.domain.observer;

import java.time.LocalDateTime;

/**
 * Event fired when an incident's status changes.
 */
public record StatusChangedEvent(
        Long incidentId,
        String incidentDisplayId,
        String previousStatus,
        String newStatus,
        String actorEmail,
        LocalDateTime timestamp
) implements IncidentEvent {

    @Override
    public String eventType() {
        return "STATUS_CHANGED";
    }
}
