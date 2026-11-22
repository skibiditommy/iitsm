package com.iitsm.domain.observer;

import java.time.LocalDateTime;

/**
 * Observer Pattern: Base interface for all incident domain events.
 * OOP: Abstraction — observers depend on this interface, not concrete events.
 */
public interface IncidentEvent {
    Long incidentId();
    String incidentDisplayId();
    LocalDateTime timestamp();
    String eventType();
}
