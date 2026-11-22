package com.iitsm.dto.request;

import jakarta.validation.constraints.Min;

public record UpdateSlaPolicyRequest(
        @Min(1) long responseMinutes,
        @Min(1) long resolutionMinutes
) {}
