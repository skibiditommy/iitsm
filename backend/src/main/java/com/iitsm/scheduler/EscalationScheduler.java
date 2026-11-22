package com.iitsm.scheduler;

import com.iitsm.domain.enums.EscalationLevel;
import com.iitsm.domain.enums.SlaStatus;
import com.iitsm.domain.model.AuditLogEntry;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.observer.IncidentEscalatedEvent;
import com.iitsm.domain.observer.IncidentEventPublisher;
import com.iitsm.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Automatic escalation scheduler. Escalates incidents that have breached SLA
 * through the hierarchy: Technician → Team Lead → System Admin → IT Manager.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EscalationScheduler {

    private final IncidentRepository incidentRepository;
    private final IncidentEventPublisher eventPublisher;
    private final com.iitsm.domain.escalation.EscalationChain escalationChain;

    @Scheduled(fixedRate = 120000) // every 2 minutes
    @Transactional
    public void autoEscalate() {
        List<Incident> breached = incidentRepository.findActiveSlaBreached();

        for (Incident incident : breached) {
            if (incident.getEscalationLevel() == EscalationLevel.IT_MANAGER) {
                continue; // Already at highest level
            }

            try {
                EscalationLevel previousLevel = incident.getEscalationLevel();
                String reason = "Automatic escalation due to SLA breach";

                // Delegate to GoF Chain of Responsibility
                boolean handled = escalationChain.escalate(incident, reason, "system@iitsm.org");

                if (handled) {
                    EscalationLevel newLevel = incident.getEscalationLevel();
                    incidentRepository.save(incident);

                    eventPublisher.publish(new IncidentEscalatedEvent(
                            incident.getId(), incident.getIncidentId(),
                            previousLevel.name(), newLevel.name(), reason, LocalDateTime.now()
                    ));

                    log.warn("Auto-escalated {} from {} to {} via EscalationChain due to SLA breach",
                            incident.getIncidentId(), previousLevel, newLevel);
                }
            } catch (Exception e) {
                log.error("Failed to auto-escalate {}: {}", incident.getIncidentId(), e.getMessage());
            }
        }
    }
}
