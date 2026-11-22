package com.iitsm.domain.exception;

public class TechnicianUnavailableException extends IitsmException {

    public TechnicianUnavailableException(String message) {
        super(message, "TECH_001", 409);
    }
}
