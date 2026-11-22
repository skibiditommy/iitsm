package com.iitsm.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Transactional outbox entry for asynchronous notification delivery.
 * <p>
 * Notifications are written to this table within the same transaction as the
 * domain event, then picked up and delivered by a background poller.
 * This ensures at-least-once delivery semantics.
 * </p>
 */
@Entity
@Table(name = "notification_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String recipientEmail;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    private boolean delivered = false;

    private LocalDateTime createdAt;

    private String incidentId;

    private String eventType;

    public NotificationOutbox(String channel, String recipientEmail, String subject,
                              String body, String incidentId, String eventType) {
        this.channel = channel;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.body = body;
        this.incidentId = incidentId;
        this.eventType = eventType;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Marks this notification as delivered.
     */
    public void markDelivered() {
        this.delivered = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationOutbox that = (NotificationOutbox) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "NotificationOutbox{id=" + id + ", channel='" + channel
                + "', recipient='" + recipientEmail + "', delivered=" + delivered + "}";
    }
}
