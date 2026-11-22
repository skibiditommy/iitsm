package com.iitsm.domain.exception;

public class InvalidIncidentException extends IitsmException {

    public InvalidIncidentException(String message) {
        super(message, "INC_001", 400);
    }
}
