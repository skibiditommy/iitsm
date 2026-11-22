package com.iitsm.dto.request;

import jakarta.validation.constraints.Min;

public record AdvanceTimeRequest(
        @Min(1) int minutes
) {}
