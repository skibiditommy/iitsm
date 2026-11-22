package com.iitsm.dto.request;

import java.time.LocalDateTime;

public record UpdateDeadlineRequest(
        LocalDateTime resolutionDeadline,
        LocalDateTime responseDeadline,
        Integer addMinutes,
        String reason
) {}
