package com.iitsm.domain.exception;

public class MissingSlaConfigurationException extends IitsmException {

    public MissingSlaConfigurationException(String priority, String serviceName) {
        super("No SLA configuration found for priority '" + priority
                + "' and service '" + serviceName + "'", "SLA_001", 422);
    }
}
