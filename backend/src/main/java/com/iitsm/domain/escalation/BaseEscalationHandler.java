package com.iitsm.domain.escalation;

import com.iitsm.domain.enums.EscalationLevel;
import com.iitsm.domain.model.Incident;

/**
 * GoF Chain of Responsibility Pattern: Abstract Base Handler.
 * Holds reference to the successor in the chain and provides propagation logic.
 */
public abstract class BaseEscalationHandler implements EscalationHandler {

    protected EscalationHandler next;
    protected final EscalationLevel supportedLevel;

    protected BaseEscalationHandler(EscalationLevel supportedLevel) {
        this.supportedLevel = supportedLevel;
    }

    @Override
    public void setNext(EscalationHandler next) {
        this.next = next;
    }

    protected boolean passToNext(Incident incident, String reason, String actorEmail) {
        if (next != null) {
            return next.handleEscalation(incident, reason, actorEmail);
        }
        return false;
    }
}
