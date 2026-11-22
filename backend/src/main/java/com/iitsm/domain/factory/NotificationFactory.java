package com.iitsm.domain.factory;

import com.iitsm.domain.enums.NotificationChannelType;
import com.iitsm.domain.observer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory Pattern: Builds channel-specific {@link NotificationPayload} objects from domain events.
 * <p>
 * Each event type produces one or more notification payloads targeting different
 * delivery channels (WebSocket for all, Email for critical events, SMS for SLA violations).
 */
@Component
public class NotificationFactory {

    /**
     * Builds notification payloads for the given domain event.
     *
     * @param event the incident domain event
     * @return a list of notification payloads for dispatch
     */
    public List<NotificationPayload> createPayloads(IncidentEvent event) {
        List<NotificationPayload> payloads = new ArrayList<>();

        String subject;
        String body;
        String recipientEmail;

        if (event instanceof IncidentCreatedEvent e) {
            subject = "New Incident: " + e.incidentDisplayId();
            body = "Incident '" + e.title() + "' has been created with priority " + e.priority();
            recipientEmail = e.reporterEmail();
            payloads.add(new NotificationPayload(NotificationChannelType.WEBSOCKET,
                    recipientEmail, subject, body, e.incidentDisplayId(), e.eventType()));
            payloads.add(new NotificationPayload(NotificationChannelType.EMAIL,
                    recipientEmail, subject, body, e.incidentDisplayId(), e.eventType()));

        } else if (event instanceof TechnicianAssignedEvent e) {
            subject = "Assigned: " + e.incidentDisplayId();
            body = "Incident " + e.incidentDisplayId() + " has been assigned to " + e.technicianName();
            recipientEmail = e.technicianEmail();
            payloads.add(new NotificationPayload(NotificationChannelType.WEBSOCKET,
                    recipientEmail, subject, body, e.incidentDisplayId(), e.eventType()));
            payloads.add(new NotificationPayload(NotificationChannelType.EMAIL,
                    recipientEmail, subject, body, e.incidentDisplayId(), e.eventType()));

        } else if (event instanceof StatusChangedEvent e) {
            subject = "Status Update: " + e.incidentDisplayId();
            body = "Incident " + e.incidentDisplayId() + " status changed from "
                    + e.previousStatus() + " to " + e.newStatus();
            recipientEmail = e.actorEmail();
            payloads.add(new NotificationPayload(NotificationChannelType.WEBSOCKET,
                    recipientEmail, subject, body, e.incidentDisplayId(), e.eventType()));

        } else if (event instanceof SlaApproachingEvent e) {
            subject = "⚠ SLA Warning: " + e.incidentDisplayId();
            body = e.slaType() + " SLA for incident " + e.incidentDisplayId()
                    + " will expire in " + e.minutesRemaining() + " minutes";
            recipientEmail = "admin@iitsm.org";
            payloads.add(new NotificationPayload(NotificationChannelType.WEBSOCKET,
                    recipientEmail, subject, body, e.incidentDisplayId(), e.eventType()));
            payloads.add(new NotificationPayload(NotificationChannelType.EMAIL,
                    recipientEmail, subject, body, e.incidentDisplayId(), e.eventType()));

        } else if (event instanceof SlaViolatedEvent e) {
            subject = "🚨 SLA BREACHED: " + e.incidentDisplayId();
            body = e.slaType() + " SLA for incident " + e.incidentDisplayId()
                    + " has been breached by " + e.minutesOverdue() + " minutes";
            recipientEmail = "admin@iitsm.org";
            payloads.add(new NotificationPayload(NotificationChannelType.WEBSOCKET,
                    recipientEmail, subject, body, e.incidentDisplayId(), e.eventType()));
            payloads.add(new NotificationPayload(NotificationChannelType.EMAIL,
                    recipientEmail, subject, body, e.incidentDisplayId(), e.eventType()));
            payloads.add(new NotificationPayload(NotificationChannelType.SMS,
                    recipientEmail, subject, body, e.incidentDisplayId(), e.eventType()));

        } else if (event instanceof IncidentEscalatedEvent e) {
            subject = "Escalation: " + e.incidentDisplayId();
            body = "Incident " + e.incidentDisplayId() + " escalated from "
                    + e.previousLevel() + " to " + e.newLevel() + ". Reason: " + e.reason();
            recipientEmail = "admin@iitsm.org";
            payloads.add(new NotificationPayload(NotificationChannelType.WEBSOCKET,
                    recipientEmail, subject, body, e.incidentDisplayId(), e.eventType()));
            payloads.add(new NotificationPayload(NotificationChannelType.EMAIL,
                    recipientEmail, subject, body, e.incidentDisplayId(), e.eventType()));

        } else if (event instanceof IncidentResolvedEvent e) {
            subject = "Resolved: " + e.incidentDisplayId();
            body = "Incident " + e.incidentDisplayId() + " has been resolved: " + e.resolution();
            recipientEmail = e.technicianEmail();
            payloads.add(new NotificationPayload(NotificationChannelType.WEBSOCKET,
                    recipientEmail, subject, body, e.incidentDisplayId(), e.eventType()));

        } else if (event instanceof IncidentClosedEvent e) {
            subject = "Closed: " + e.incidentDisplayId();
            body = "Incident " + e.incidentDisplayId() + " has been closed.";
            recipientEmail = "admin@iitsm.org";
            payloads.add(new NotificationPayload(NotificationChannelType.WEBSOCKET,
                    recipientEmail, subject, body, e.incidentDisplayId(), e.eventType()));

        } else {
            // Default for any unhandled event types
            subject = "Event: " + event.incidentDisplayId();
            body = event.eventType() + " occurred on incident " + event.incidentDisplayId();
            recipientEmail = "admin@iitsm.org";
            payloads.add(new NotificationPayload(NotificationChannelType.WEBSOCKET,
                    recipientEmail, subject, body, event.incidentDisplayId(), event.eventType()));
        }

        return payloads;
    }
}
