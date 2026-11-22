package com.iitsm.domain.state;

import com.iitsm.domain.enums.Status;

/**
 * Concrete state for a verified Incident (Status = VERIFIED).
 * Permits: close() → ClosedState.
 */
public class VerifiedState implements IncidentState {

    @Override
    public Status getStatus() {
        return Status.VERIFIED;
    }

    @Override
    public IncidentState close() {
        return new ClosedState();
    }
}
