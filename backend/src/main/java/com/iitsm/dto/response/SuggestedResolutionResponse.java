package com.iitsm.dto.response;

public record SuggestedResolutionResponse(String incidentId, String title, String resolution,
                                            String rootCause, double similarityScore) {}
