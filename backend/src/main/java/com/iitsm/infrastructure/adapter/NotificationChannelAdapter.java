package com.iitsm.infrastructure.adapter;

import com.iitsm.domain.enums.NotificationChannelType;
import com.iitsm.domain.factory.NotificationPayload;

/**
 * Adapter Pattern: Interface for notification delivery channels.
 * Each implementation handles a specific channel (WebSocket, Email, SMS).
 */
public interface NotificationChannelAdapter {

    /**
     * Returns whether this adapter supports the given channel type.
     */
    boolean supports(NotificationChannelType channelType);

    /**
     * Sends a notification payload through this adapter's channel.
     */
    void send(NotificationPayload payload);
}
