package com.iitsm.domain.state;

import com.iitsm.domain.enums.Status;

/**
 * State Pattern: Concrete state representing an incident waiting for employee clarification.
 * While in this state, the SLA clock is paused to ensure fair technician evaluation.
 * Permits: resumeWork() → InProgressState, resolve() → ResolvedState, escalate() → EscalatedState.
 */
public class PendingUserState implements IncidentState {

    @Override
    public Status getStatus() {
        return Status.PENDING_USER;
    }

    @Override
    public IncidentState resumeWork() {
        return new InProgressState();
    }

    @Override
    public IncidentState resolve() {
        return new ResolvedState();
    }

    @Override
    public IncidentState escalate() {
        return new EscalatedState();
    }
}
