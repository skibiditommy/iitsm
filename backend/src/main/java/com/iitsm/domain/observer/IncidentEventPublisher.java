package com.iitsm.domain.observer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Observer Pattern: Publisher that broadcasts incident events to all registered observers.
 * OOP: Open/Closed Principle — new observers are added via Spring DI without modifying this class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentEventPublisher {

    private final List<IncidentEventObserver> observers;

    /**
     * Publishes an event to all registered observers.
     * Each observer is called independently; failures in one observer do not affect others.
     *
     * @param event the incident event to publish
     */
    public void publish(IncidentEvent event) {
        log.info("Publishing event: {} for incident {}", event.eventType(), event.incidentDisplayId());
        for (IncidentEventObserver observer : observers) {
            try {
                observer.onEvent(event);
            } catch (Exception e) {
                log.error("Observer {} failed processing event {}: {}",
                        observer.getClass().getSimpleName(), event.eventType(), e.getMessage(), e);
            }
        }
    }
}
