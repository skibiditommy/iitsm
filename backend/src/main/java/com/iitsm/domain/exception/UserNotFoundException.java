package com.iitsm.domain.exception;

public class UserNotFoundException extends IitsmException {

    public UserNotFoundException(String message) {
        super(message, "USER_001", 404);
    }
}
