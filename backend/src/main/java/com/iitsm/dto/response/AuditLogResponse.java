package com.iitsm.dto.response;

import java.time.LocalDateTime;

public record AuditLogResponse(Long id, LocalDateTime timestamp, String actorEmail,
                                 String action, String previousState, String newState,
                                 String description) {}
