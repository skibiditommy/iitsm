package com.iitsm.infrastructure.adapter;

import com.iitsm.domain.enums.NotificationChannelType;
import com.iitsm.domain.factory.NotificationPayload;
import com.iitsm.domain.model.NotificationOutbox;
import com.iitsm.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter Pattern: Simulates email delivery by logging and persisting to the outbox.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationAdapter implements NotificationChannelAdapter {

    private final NotificationOutboxRepository outboxRepository;

    @Override
    public boolean supports(NotificationChannelType channelType) {
        return channelType == NotificationChannelType.EMAIL;
    }

    @Override
    public void send(NotificationPayload payload) {
        log.info("📧 [EMAIL SIMULATED] To: {} | Subject: {} | Body: {}",
                payload.recipientEmail(), payload.subject(), payload.body());

        NotificationOutbox outboxEntry = new NotificationOutbox(
                "EMAIL", payload.recipientEmail(), payload.subject(),
                payload.body(), payload.incidentId(), payload.eventType()
        );
        outboxRepository.save(outboxEntry);
    }
}
