package com.iitsm.domain.escalation;

import com.iitsm.domain.model.Incident;

/**
 * GoF Chain of Responsibility Pattern: Handler Interface.
 * Defines the contract for processing incident escalation tiers.
 */
public interface EscalationHandler {
    void setNext(EscalationHandler next);
    boolean handleEscalation(Incident incident, String reason, String actorEmail);
}
