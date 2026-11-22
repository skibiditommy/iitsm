package com.iitsm.domain.factory;

import com.iitsm.domain.enums.Impact;
import com.iitsm.domain.enums.Priority;
import com.iitsm.domain.enums.Urgency;
import com.iitsm.domain.exception.InvalidIncidentException;
import com.iitsm.domain.model.*;
import com.iitsm.domain.strategy.PriorityCalculationStrategy;
import com.iitsm.domain.valueobject.PriorityScore;
import com.iitsm.repository.IncidentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Factory Pattern: Creates fully-initialized {@link Incident} domain objects.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Validates all required fields</li>
 *   <li>Generates collision-free incident IDs (INC-XXXX) backed by sequence / repository checks</li>
 *   <li>Invokes the active {@link PriorityCalculationStrategy} to compute priority</li>
 *   <li>Computes SLA deadlines based on calculated priority</li>
 *   <li>Attaches attachments and initial audit trail entry</li>
 * </ul>
 */
@Slf4j
@Component
public class IncidentFactory {

    private final IncidentRepository incidentRepository;
    private final AtomicLong counter = new AtomicLong(1015);

    @Autowired
    public IncidentFactory(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    public IncidentFactory() {
        this.incidentRepository = null;
    }

    public Incident createIncident(String title, String description,
                                   Employee reporter, IncidentCategory category,
                                   ITService affectedService, Impact impact, Urgency urgency,
                                   int affectedUsersCount,
                                   PriorityCalculationStrategy priorityStrategy) {
        return createIncident(title, description, reporter, category, affectedService,
                impact, urgency, affectedUsersCount, priorityStrategy, null, null, null);
    }

    public Incident createIncident(String title, String description,
                                   Employee reporter, IncidentCategory category,
                                   ITService affectedService, Impact impact, Urgency urgency,
                                   int affectedUsersCount,
                                   PriorityCalculationStrategy priorityStrategy,
                                   String attachmentName, String attachmentType, String attachmentData) {

        // Validate required fields
        if (title == null || title.isBlank()) {
            throw new InvalidIncidentException("Incident title must not be blank");
        }
        if (reporter == null) {
            throw new InvalidIncidentException("Reporter must not be null");
        }
        if (impact == null || urgency == null) {
            throw new InvalidIncidentException("Impact and Urgency must not be null");
        }

        // Generate collision-free incident ID
        String incidentId = generateUniqueIncidentId();

        // Compute priority using the active strategy
        int serviceCriticality = affectedService != null ? affectedService.getBusinessCriticality() : 1;
        PriorityScore priorityScore = priorityStrategy.calculate(
                impact.getValue(), urgency.getValue(), serviceCriticality, affectedUsersCount
        );
        Priority calculatedPriority = priorityScore.getPriority();

        // Compute SLA deadlines from priority
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime responseDeadline = now.plusMinutes(calculatedPriority.getResponseMinutes());
        LocalDateTime resolutionDeadline = now.plusMinutes(calculatedPriority.getResolutionMinutes());

        // Create the incident
        Incident incident = new Incident(
                incidentId, title, description,
                reporter, category, affectedService,
                impact, urgency, affectedUsersCount,
                priorityScore, calculatedPriority,
                responseDeadline, resolutionDeadline
        );

        if (attachmentData != null && !attachmentData.isBlank()) {
            incident.setAttachment(attachmentName, attachmentType, attachmentData);
        }

        // Attach initial audit entry
        AuditLogEntry initialAudit = new AuditLogEntry(
                reporter.getEmail(),
                "INCIDENT_CREATED",
                null,
                "NEW",
                "Incident '" + title + "' created with priority " + calculatedPriority
        );
        incident.addAuditEntry(initialAudit);

        log.info("IncidentFactory created incident {} with priority {} (strategy: {})",
                incidentId, calculatedPriority, priorityStrategy.getStrategyName());

        return incident;
    }

    private synchronized String generateUniqueIncidentId() {
        if (incidentRepository != null) {
            try {
                String id;
                do {
                    id = "INC-" + incidentRepository.nextDisplayNumber();
                } while (incidentRepository.existsByIncidentId(id));
                return id;
            } catch (Exception e) {
                log.warn("Sequence nextDisplayNumber failed, falling back to counter: {}", e.getMessage());
            }
        }
        long next = counter.incrementAndGet();
        if (incidentRepository != null) {
            while (incidentRepository.existsByIncidentId("INC-" + next)) {
                next = counter.incrementAndGet();
            }
        }
        return "INC-" + next;
    }
}
