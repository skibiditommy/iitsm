package com.iitsm.domain.state;

import com.iitsm.domain.enums.Status;

/**
 * Concrete state for an assigned Incident (Status = ASSIGNED).
 * Permits: acknowledge() → AcknowledgedState, escalate() → EscalatedState, reassign() → AssignedState.
 */
public class AssignedState implements IncidentState {

    @Override
    public Status getStatus() {
        return Status.ASSIGNED;
    }

    @Override
    public IncidentState acknowledge() {
        return new AcknowledgedState();
    }

    @Override
    public IncidentState escalate() {
        return new EscalatedState();
    }

    @Override
    public IncidentState reassign() {
        return new AssignedState();
    }
}
