package com.iitsm.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record VerifyIncidentRequest(
        @NotBlank(message = "Feedback is required") String feedback,
        @Min(1) @Max(5) int rating
) {}
