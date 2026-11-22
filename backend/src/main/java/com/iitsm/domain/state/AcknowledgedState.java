package com.iitsm.domain.state;

import com.iitsm.domain.enums.Status;

/**
 * Concrete state for an acknowledged Incident (Status = ACKNOWLEDGED).
 * Permits: startInvestigation() → InProgressState, escalate() → EscalatedState, reassign() → AssignedState.
 */
public class AcknowledgedState implements IncidentState {

    @Override
    public Status getStatus() {
        return Status.ACKNOWLEDGED;
    }

    @Override
    public IncidentState startInvestigation() {
        return new InProgressState();
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
