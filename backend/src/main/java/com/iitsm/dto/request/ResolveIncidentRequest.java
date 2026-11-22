package com.iitsm.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResolveIncidentRequest(
        @NotBlank(message = "Root cause is required") String rootCause,
        @NotBlank(message = "Resolution is required") String resolution,
        String preventiveMeasures
) {}
