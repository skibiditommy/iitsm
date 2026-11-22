package com.iitsm.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, String channel, String recipientEmail,
                                     String subject, String body, boolean delivered,
                                     String incidentId, String eventType,
                                     LocalDateTime createdAt) {}
