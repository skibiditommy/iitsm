package com.iitsm.dto.request;

import jakarta.validation.constraints.NotBlank;

public record StrategyUpdateRequest(
        @NotBlank(message = "Strategy type is required") String strategyType,
        @NotBlank(message = "Strategy name is required") String strategyName
) {}
