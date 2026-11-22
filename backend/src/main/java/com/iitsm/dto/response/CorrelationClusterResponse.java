package com.iitsm.dto.response;

import java.util.List;

public record CorrelationClusterResponse(String clusterName, String commonPattern,
                                           List<String> incidentIds) {}
