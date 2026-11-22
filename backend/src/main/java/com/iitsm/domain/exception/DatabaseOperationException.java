package com.iitsm.domain.exception;

public class DatabaseOperationException extends IitsmException {

    public DatabaseOperationException(String message, Throwable cause) {
        super(message, "DB_001", 503, cause);
    }
}
