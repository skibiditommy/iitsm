package com.iitsm.domain.exception;

public class ExternalServiceFailureException extends IitsmException {

    public ExternalServiceFailureException(String serviceName, Throwable cause) {
        super("External service failure: " + serviceName, "EXT_001", 502, cause);
    }
}
