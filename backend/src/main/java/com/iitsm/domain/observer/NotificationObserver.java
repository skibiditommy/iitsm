package com.iitsm.domain.observer;

import com.iitsm.domain.factory.NotificationFactory;
import com.iitsm.domain.factory.NotificationPayload;
import com.iitsm.infrastructure.adapter.NotificationChannelAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Observer Pattern: Dispatches notifications to channel adapters when domain events occur.
 * <p>
 * Uses the {@link NotificationFactory} to create channel-specific payloads,
 * then delegates delivery to all registered {@link NotificationChannelAdapter}s.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class NotificationObserver implements IncidentEventObserver {

    private final NotificationFactory notificationFactory;
    private final List<NotificationChannelAdapter> channelAdapters;

    @Override
    public void onEvent(IncidentEvent event) {
        try {
            List<NotificationPayload> payloads = notificationFactory.createPayloads(event);
            for (NotificationPayload payload : payloads) {
                for (NotificationChannelAdapter adapter : channelAdapters) {
                    if (adapter.supports(payload.channel())) {
                        adapter.send(payload);
                    }
                }
            }
            log.debug("NotificationObserver: dispatched {} payloads for event {} on incident {}",
                    payloads.size(), event.eventType(), event.incidentDisplayId());
        } catch (Exception e) {
            log.error("NotificationObserver: failed to dispatch notifications for event {}: {}",
                    event.eventType(), e.getMessage());
        }
    }
}
