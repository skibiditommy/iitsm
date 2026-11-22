package com.iitsm.infrastructure.adapter;

import com.iitsm.domain.enums.NotificationChannelType;
import com.iitsm.domain.factory.NotificationPayload;
import com.iitsm.domain.model.NotificationOutbox;
import com.iitsm.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter Pattern: Simulates SMS delivery by logging and persisting to the outbox.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsNotificationAdapter implements NotificationChannelAdapter {

    private final NotificationOutboxRepository outboxRepository;

    @Override
    public boolean supports(NotificationChannelType channelType) {
        return channelType == NotificationChannelType.SMS;
    }

    @Override
    public void send(NotificationPayload payload) {
        log.info("📱 [SMS SIMULATED] To: {} | Message: {}",
                payload.recipientEmail(), payload.body());

        NotificationOutbox outboxEntry = new NotificationOutbox(
                "SMS", payload.recipientEmail(), payload.subject(),
                payload.body(), payload.incidentId(), payload.eventType()
        );
        outboxRepository.save(outboxEntry);
    }
}
