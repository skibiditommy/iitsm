package com.iitsm.domain.exception;

public class InvalidLoginException extends IitsmException {

    public InvalidLoginException(String message) {
        super(message, "AUTH_001", 401);
    }
}
