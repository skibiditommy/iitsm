package com.iitsm.domain.exception;

public class IncidentNotFoundException extends IitsmException {

    public IncidentNotFoundException(String message) {
        super(message, "INC_004", 404);
    }
}
