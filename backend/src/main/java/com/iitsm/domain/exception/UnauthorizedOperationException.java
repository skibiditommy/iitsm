package com.iitsm.domain.exception;

public class UnauthorizedOperationException extends IitsmException {

    public UnauthorizedOperationException(String message) {
        super(message, "AUTH_002", 403);
    }
}
