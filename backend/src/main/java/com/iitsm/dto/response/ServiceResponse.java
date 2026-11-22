package com.iitsm.dto.response;

import java.util.List;

public record ServiceResponse(Long id, String name, String description,
                                int businessCriticality, List<ServiceSummary> upstreamDependencies,
                                List<ServiceSummary> downstreamDependencies,
                                int blastRadiusCount) {}
