package com.iitsm.domain.state;

import com.iitsm.domain.enums.Status;

/**
 * Concrete state for an escalated Incident (Status = ESCALATED).
 * Permits: assign() → AssignedState, reassign() → AssignedState.
 */
public class EscalatedState implements IncidentState {

    @Override
    public Status getStatus() {
        return Status.ESCALATED;
    }

    @Override
    public IncidentState assign() {
        return new AssignedState();
    }

    @Override
    public IncidentState reassign() {
        return new AssignedState();
    }
}
