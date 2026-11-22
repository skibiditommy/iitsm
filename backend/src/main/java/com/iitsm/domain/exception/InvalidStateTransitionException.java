package com.iitsm.domain.exception;

public class InvalidStateTransitionException extends IitsmException {

    private final String currentState;
    private final String attemptedAction;

    public InvalidStateTransitionException(String currentState, String attemptedAction) {
        super("Cannot perform '" + attemptedAction + "' on incident in state '" + currentState + "'",
                "INC_002", 409);
        this.currentState = currentState;
        this.attemptedAction = attemptedAction;
    }

    public InvalidStateTransitionException(String currentState, String attemptedAction, String reason) {
        super("Cannot transition from '" + currentState + "' to '" + attemptedAction + "': " + reason,
                "INC_002", 409);
        this.currentState = currentState;
        this.attemptedAction = attemptedAction;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getAttemptedAction() {
        return attemptedAction;
    }
}
