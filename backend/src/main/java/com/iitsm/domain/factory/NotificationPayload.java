package com.iitsm.domain.factory;

import com.iitsm.domain.enums.NotificationChannelType;

/**
 * Immutable record representing a notification payload ready for dispatch.
 *
 * @param channel        the delivery channel (WEBSOCKET, EMAIL, SMS)
 * @param recipientEmail the recipient's email address
 * @param subject        the notification subject line
 * @param body           the notification body text
 * @param incidentId     the related incident display ID (e.g. "INC-1001")
 * @param eventType      the domain event type that triggered this notification
 */
public record NotificationPayload(
        NotificationChannelType channel,
        String recipientEmail,
        String subject,
        String body,
        String incidentId,
        String eventType
) {
}
