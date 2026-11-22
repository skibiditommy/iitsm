package com.iitsm.domain.exception;

public class ServiceNotFoundException extends IitsmException {

    public ServiceNotFoundException(String serviceName) {
        super("Service not found: " + serviceName, "SVC_001", 404);
    }

    public ServiceNotFoundException(Long id) {
        super("Service not found with id: " + id, "SVC_001", 404);
    }
}
