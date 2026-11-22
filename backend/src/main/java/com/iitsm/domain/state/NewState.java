package com.iitsm.domain.state;

import com.iitsm.domain.enums.Status;

/**
 * Concrete state for a newly created Incident (Status = NEW).
 * Permits: classify() → ClassifiedState, escalate() → EscalatedState.
 */
public class NewState implements IncidentState {

    @Override
    public Status getStatus() {
        return Status.NEW;
    }

    @Override
    public IncidentState classify() {
        return new ClassifiedState();
    }

    @Override
    public IncidentState escalate() {
        return new EscalatedState();
    }
}
