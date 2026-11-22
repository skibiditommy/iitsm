package com.iitsm.domain.state;

import com.iitsm.domain.enums.Status;

/**
 * Terminal state for a closed Incident (Status = CLOSED).
 * No transitions are permitted — all default methods throw InvalidStateTransitionException.
 */
public class ClosedState implements IncidentState {

    @Override
    public Status getStatus() {
        return Status.CLOSED;
    }
}
