package com.iitsm.dto.response;

public record TechnicianWorkloadResponse(Long id, String name, String email,
                                           int currentWorkload, int maxWorkload,
                                           double utilizationPercent, boolean available) {}
