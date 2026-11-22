package com.iitsm.domain.observer;

/**
 * Observer Pattern: Interface for event observers.
 * OOP: Abstraction + Polymorphism — new observers can be added without modifying the publisher.
 */
public interface IncidentEventObserver {
    void onEvent(IncidentEvent event);
}
