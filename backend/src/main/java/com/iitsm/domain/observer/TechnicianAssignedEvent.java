package com.iitsm.domain.observer;

import java.time.LocalDateTime;

/**
 * Event fired when a technician is assigned to an incident.
 */
public record TechnicianAssignedEvent(
        Long incidentId,
        String incidentDisplayId,
        String technicianEmail,
        String technicianName,
        LocalDateTime timestamp
) implements IncidentEvent {

    @Override
    public String eventType() {
        return "TECHNICIAN_ASSIGNED";
    }
}
