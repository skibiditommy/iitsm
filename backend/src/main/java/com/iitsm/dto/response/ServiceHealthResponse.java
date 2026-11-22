package com.iitsm.dto.response;

public record ServiceHealthResponse(Long serviceId, String serviceName, int businessCriticality,
                                      long activeIncidentCount, long slaBreachedCount,
                                      int blastRadiusSize, String healthStatus) {}
