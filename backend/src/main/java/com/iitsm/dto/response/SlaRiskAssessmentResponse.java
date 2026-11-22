package com.iitsm.dto.response;

public record SlaRiskAssessmentResponse(String incidentId, String responseSlaStatus,
                                          String resolutionSlaStatus, long responseMinutesRemaining,
                                          long resolutionMinutesRemaining, String riskLevel) {}
