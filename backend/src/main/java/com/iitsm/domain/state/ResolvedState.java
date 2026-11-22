package com.iitsm.domain.state;

import com.iitsm.domain.enums.Status;

/**
 * Concrete state for a resolved Incident (Status = RESOLVED).
 * Permits: verify() → VerifiedState, startInvestigation() → InProgressState (reopen).
 */
public class ResolvedState implements IncidentState {

    @Override
    public Status getStatus() {
        return Status.RESOLVED;
    }

    @Override
    public IncidentState verify() {
        return new VerifiedState();
    }

    @Override
    public IncidentState startInvestigation() {
        return new InProgressState();
    }
}
