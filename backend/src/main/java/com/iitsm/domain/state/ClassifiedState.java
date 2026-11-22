package com.iitsm.domain.state;

import com.iitsm.domain.enums.Status;

/**
 * Concrete state for a classified Incident (Status = CLASSIFIED).
 * Permits: assign() → AssignedState, escalate() → EscalatedState.
 */
public class ClassifiedState implements IncidentState {

    @Override
    public Status getStatus() {
        return Status.CLASSIFIED;
    }

    @Override
    public IncidentState assign() {
        return new AssignedState();
    }

    @Override
    public IncidentState escalate() {
        return new EscalatedState();
    }
}
