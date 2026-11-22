package com.iitsm.domain.state;

import com.iitsm.domain.enums.Status;
import com.iitsm.domain.exception.InvalidStateTransitionException;

/**
 * State Pattern: Defines all possible lifecycle transitions for an Incident.
 * Default implementations throw InvalidStateTransitionException so each
 * concrete state only overrides the transitions it permits.
 * OOP: Polymorphism — the Incident delegates behavior to its current state object.
 * OOP: Encapsulation — transition rules are encapsulated within each state.
 */
public interface IncidentState {
    Status getStatus();

    default IncidentState classify() {
        throw new InvalidStateTransitionException(getStatus().name(), "classify");
    }

    default IncidentState assign() {
        throw new InvalidStateTransitionException(getStatus().name(), "assign");
    }

    default IncidentState acknowledge() {
        throw new InvalidStateTransitionException(getStatus().name(), "acknowledge");
    }

    default IncidentState startInvestigation() {
        throw new InvalidStateTransitionException(getStatus().name(), "startInvestigation");
    }

    default IncidentState requestInfo() {
        throw new InvalidStateTransitionException(getStatus().name(), "requestInfo");
    }

    default IncidentState resumeWork() {
        throw new InvalidStateTransitionException(getStatus().name(), "resumeWork");
    }

    default IncidentState resolve() {
        throw new InvalidStateTransitionException(getStatus().name(), "resolve");
    }

    default IncidentState verify() {
        throw new InvalidStateTransitionException(getStatus().name(), "verify");
    }

    default IncidentState close() {
        throw new InvalidStateTransitionException(getStatus().name(), "close");
    }

    default IncidentState escalate() {
        throw new InvalidStateTransitionException(getStatus().name(), "escalate");
    }

    default IncidentState reassign() {
        throw new InvalidStateTransitionException(getStatus().name(), "reassign");
    }
}
