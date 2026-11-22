package com.iitsm.domain.observer;

import com.iitsm.domain.model.AuditLogEntry;
import com.iitsm.domain.model.Incident;
import com.iitsm.repository.AuditLogEntryRepository;
import com.iitsm.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Observer Pattern: Persists an {@link AuditLogEntry} for every incident domain event.
 * <p>
 * OOP: Open/Closed Principle — this observer is registered via Spring DI without modifying
 * the {@link IncidentEventPublisher}. It reacts to all event types polymorphically.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AuditTrailObserver implements IncidentEventObserver {

    private final IncidentRepository incidentRepository;
    private final AuditLogEntryRepository auditLogEntryRepository;

    @Override
    public void onEvent(IncidentEvent event) {
        try {
            String action = event.eventType();
            String description = buildDescription(event);
            String actorEmail = extractActorEmail(event);
            String previousState = extractPreviousState(event);
            String newState = extractNewState(event);

            AuditLogEntry entry = new AuditLogEntry(
                    actorEmail, action, previousState, newState, description
            );

            // Link to incident if exists in DB
            incidentRepository.findById(event.incidentId()).ifPresent(incident -> {
                incident.addAuditEntry(entry);
                incidentRepository.save(incident);
            });

            log.debug("AuditTrailObserver: persisted audit entry for event {} on incident {}",
                    action, event.incidentDisplayId());
        } catch (Exception e) {
            log.error("AuditTrailObserver: failed to persist audit entry for event {}: {}",
                    event.eventType(), e.getMessage());
        }
    }

    private String buildDescription(IncidentEvent event) {
        if (event instanceof IncidentCreatedEvent e) {
            return "Incident created: " + e.title() + " (Priority: " + e.priority() + ")";
        } else if (event instanceof IncidentClassifiedEvent e) {
            return "Classified into category: " + e.category() + ", service: " + e.service();
        } else if (event instanceof TechnicianAssignedEvent e) {
            return "Assigned to technician: " + e.technicianName() + " (" + e.technicianEmail() + ")";
        } else if (event instanceof StatusChangedEvent e) {
            return "Status changed from " + e.previousStatus() + " to " + e.newStatus();
        } else if (event instanceof SlaApproachingEvent e) {
            return e.slaType() + " SLA approaching: " + e.minutesRemaining() + " minutes remaining";
        } else if (event instanceof SlaViolatedEvent e) {
            return e.slaType() + " SLA breached: " + e.minutesOverdue() + " minutes overdue";
        } else if (event instanceof IncidentEscalatedEvent e) {
            return "Escalated from " + e.previousLevel() + " to " + e.newLevel() + ": " + e.reason();
        } else if (event instanceof IncidentResolvedEvent e) {
            return "Resolved: " + e.resolution();
        } else if (event instanceof IncidentClosedEvent) {
            return "Incident closed";
        } else if (event instanceof PriorityChangedEvent e) {
            return "Priority changed from " + e.oldPriority() + " to " + e.newPriority()
                    + " (score: " + e.score() + ")";
        }
        return event.eventType();
    }

    private String extractActorEmail(IncidentEvent event) {
        if (event instanceof IncidentCreatedEvent e) return e.reporterEmail();
        if (event instanceof TechnicianAssignedEvent e) return e.technicianEmail();
        if (event instanceof StatusChangedEvent e) return e.actorEmail();
        if (event instanceof IncidentResolvedEvent e) return e.technicianEmail();
        return "system@iitsm.org";
    }

    private String extractPreviousState(IncidentEvent event) {
        if (event instanceof StatusChangedEvent e) return e.previousStatus();
        if (event instanceof IncidentEscalatedEvent e) return e.previousLevel();
        if (event instanceof PriorityChangedEvent e) return e.oldPriority();
        return null;
    }

    private String extractNewState(IncidentEvent event) {
        if (event instanceof StatusChangedEvent e) return e.newStatus();
        if (event instanceof IncidentEscalatedEvent e) return e.newLevel();
        if (event instanceof PriorityChangedEvent e) return e.newPriority();
        if (event instanceof IncidentCreatedEvent) return "NEW";
        if (event instanceof IncidentClosedEvent) return "CLOSED";
        if (event instanceof IncidentResolvedEvent) return "RESOLVED";
        return null;
    }
}
