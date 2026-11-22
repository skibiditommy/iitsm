package com.iitsm.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateServiceRequest(
        @NotBlank(message = "Service name is required") String name,
        String description,
        @Min(1) @Max(5) int businessCriticality,
        java.util.List<Long> upstreamDependencyIds
) {}
