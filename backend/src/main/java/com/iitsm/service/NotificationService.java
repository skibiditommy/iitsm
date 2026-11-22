package com.iitsm.service;

import com.iitsm.domain.model.NotificationOutbox;
import com.iitsm.dto.response.NotificationResponse;
import com.iitsm.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationOutboxRepository outboxRepository;

    public List<NotificationResponse> getNotificationsForUser(String email) {
        return outboxRepository.findByRecipientEmailOrderByCreatedAtDesc(email)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<NotificationResponse> getUndelivered() {
        return outboxRepository.findByDeliveredFalseOrderByCreatedAtAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private NotificationResponse toResponse(NotificationOutbox n) {
        return new NotificationResponse(n.getId(), n.getChannel(), n.getRecipientEmail(),
                n.getSubject(), n.getBody(), n.isDelivered(), n.getIncidentId(),
                n.getEventType(), n.getCreatedAt());
    }
}
