package com.iitsm.domain.state;

import com.iitsm.domain.enums.Status;

/**
 * Concrete state for an in-progress Incident (Status = IN_PROGRESS).
 * Permits: resolve() → ResolvedState, escalate() → EscalatedState, reassign() → AssignedState, requestInfo() → PendingUserState.
 */
public class InProgressState implements IncidentState {

    @Override
    public Status getStatus() {
        return Status.IN_PROGRESS;
    }

    @Override
    public IncidentState requestInfo() {
        return new PendingUserState();
    }

    @Override
    public IncidentState resolve() {
        return new ResolvedState();
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
