package com.iitsm.infrastructure.adapter;

import com.iitsm.domain.enums.NotificationChannelType;
import com.iitsm.domain.factory.NotificationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Adapter Pattern: Delivers notifications via WebSocket (STOMP) to connected clients.
 * Pushes messages to /topic/notifications/{email}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InAppWebSocketAdapter implements NotificationChannelAdapter {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public boolean supports(NotificationChannelType channelType) {
        return channelType == NotificationChannelType.WEBSOCKET;
    }

    @Override
    public void send(NotificationPayload payload) {
        String destination = "/topic/notifications/" + payload.recipientEmail();
        Map<String, Object> message = Map.of(
                "subject", payload.subject(),
                "body", payload.body(),
                "incidentId", payload.incidentId(),
                "eventType", payload.eventType(),
                "timestamp", java.time.LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend(destination, message);
        log.info("WebSocket notification sent to {} at {}", payload.recipientEmail(), destination);
    }
}
