package com.iitsm.dto.request;

import jakarta.validation.constraints.NotNull;

public record ClassifyIncidentRequest(
        @NotNull(message = "Category ID is required") Long categoryId,
        @NotNull(message = "Service ID is required") Long serviceId
) {}
