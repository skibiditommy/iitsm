package com.iitsm.dto.response;

public record SlaPolicyResponse(Long id, String priority, Long serviceId,
                                  String serviceName, long responseMinutes, long resolutionMinutes) {}
