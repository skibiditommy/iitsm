package com.iitsm.domain.state;

import com.iitsm.domain.enums.Status;

/**
 * Factory utility to reconstitute the correct IncidentState object from a persisted Status enum.
 * Used when loading Incidents from the database to restore their runtime state behavior.
 */
public final class IncidentStateFactory {

    private IncidentStateFactory() {
        // Utility class — prevent instantiation
    }

    /**
     * Maps a Status enum value to its corresponding IncidentState implementation.
     *
     * @param status the persisted status enum
     * @return the matching IncidentState instance
     */
    public static IncidentState fromStatus(Status status) {
        return switch (status) {
            case NEW -> new NewState();
            case CLASSIFIED -> new ClassifiedState();
            case ASSIGNED -> new AssignedState();
            case ACKNOWLEDGED -> new AcknowledgedState();
            case IN_PROGRESS -> new InProgressState();
            case PENDING_USER -> new PendingUserState();
            case RESOLVED -> new ResolvedState();
            case VERIFIED -> new VerifiedState();
            case CLOSED -> new ClosedState();
            case ESCALATED -> new EscalatedState();
        };
    }
}
