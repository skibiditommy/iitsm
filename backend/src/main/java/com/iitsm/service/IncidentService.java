package com.iitsm.service;

import com.iitsm.config.StrategyConfigService;
import com.iitsm.domain.enums.*;
import com.iitsm.domain.exception.*;
import com.iitsm.domain.factory.IncidentFactory;
import com.iitsm.domain.model.*;
import com.iitsm.domain.observer.*;
import com.iitsm.dto.request.*;
import com.iitsm.dto.response.*;
import com.iitsm.infrastructure.adapter.AiAssistantPort;
import com.iitsm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final EmployeeRepository employeeRepository;
    private final TechnicianRepository technicianRepository;
    private final IncidentCategoryRepository categoryRepository;
    private final ITServiceRepository serviceRepository;
    private final IncidentFactory incidentFactory;
    private final StrategyConfigService strategyConfigService;
    private final IncidentEventPublisher eventPublisher;
    private final AiAssistantPort aiAssistant;

    @Transactional
    public IncidentResponse createIncident(CreateIncidentRequest request, String reporterEmail) {
        Employee reporter = employeeRepository.findByEmail(reporterEmail)
                .orElseThrow(() -> new UserNotFoundException(reporterEmail));

        IncidentCategory category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));
        }

        ITService service = null;
        if (request.affectedServiceId() != null) {
            service = serviceRepository.findById(request.affectedServiceId())
                    .orElseThrow(() -> new ServiceNotFoundException(request.affectedServiceId()));
        }

        Impact impact = Impact.valueOf(request.impact().toUpperCase());
        Urgency urgency = Urgency.valueOf(request.urgency().toUpperCase());

        Incident incident = incidentFactory.createIncident(
                request.title(), request.description(), reporter, category, service,
                impact, urgency, request.affectedUsersCount(),
                strategyConfigService.getActivePriorityStrategy(),
                request.attachmentName(), request.attachmentType(), request.attachmentData()
        );

        incident = incidentRepository.save(incident);

        eventPublisher.publish(new IncidentCreatedEvent(
                incident.getId(), incident.getIncidentId(), incident.getTitle(),
                reporterEmail, incident.getCalculatedPriority().name(), LocalDateTime.now()
        ));

        log.info("Created incident: {} by {}", incident.getIncidentId(), reporterEmail);
        return toIncidentResponse(incident);
    }

    @Transactional
    public IncidentResponse classifyIncident(Long incidentId, ClassifyIncidentRequest request, String actorEmail) {
        Incident incident = findIncidentById(incidentId);
        IncidentCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));
        ITService service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> new ServiceNotFoundException(request.serviceId()));

        Status oldStatus = incident.getStatus();
        incident.classify(category, service);
        incident.addAuditEntry(new AuditLogEntry(actorEmail, "CLASSIFIED",
                oldStatus.name(), Status.CLASSIFIED.name(),
                "Classified into " + category.getName() + " / " + service.getName()));
        incidentRepository.save(incident);

        eventPublisher.publish(new IncidentClassifiedEvent(
                incident.getId(), incident.getIncidentId(),
                category.getName(), service.getName(), LocalDateTime.now()
        ));

        return toIncidentResponse(incident);
    }

    @Transactional
    public IncidentResponse assignTechnician(Long incidentId, AssignTechnicianRequest request, String actorEmail) {
        Incident incident = findIncidentById(incidentId);

        Technician technician;
        if (request != null && request.technicianId() != null) {
            technician = technicianRepository.findById(request.technicianId())
                    .orElseThrow(() -> new TechnicianUnavailableException("Technician not found: " + request.technicianId()));
        } else {
            // Auto-assign using active strategy
            List<Technician> available = technicianRepository.findAvailableTechnicians();
            technician = strategyConfigService.getActiveAssignmentStrategy()
                    .assignTechnician(incident, available)
                    .orElseThrow(() -> new TechnicianUnavailableException("No suitable technician available"));
        }

        if (!technician.isAvailable()) {
            throw new TechnicianUnavailableException("Technician " + technician.getEmail() + " is unavailable");
        }

        Status oldStatus = incident.getStatus();
        incident.assignTechnician(technician);
        incident.addAuditEntry(new AuditLogEntry(actorEmail, "ASSIGNED",
                oldStatus.name(), Status.ASSIGNED.name(),
                "Assigned to " + technician.getFullName()));
        incidentRepository.save(incident);
        technicianRepository.save(technician);

        eventPublisher.publish(new TechnicianAssignedEvent(
                incident.getId(), incident.getIncidentId(),
                technician.getEmail(), technician.getFullName(), LocalDateTime.now()
        ));

        return toIncidentResponse(incident);
    }

    @Transactional
    public IncidentResponse acknowledgeIncident(Long incidentId, String technicianEmail) {
        Incident incident = findIncidentById(incidentId);
        Technician technician = technicianRepository.findByEmail(technicianEmail)
                .orElseThrow(() -> new UserNotFoundException(technicianEmail));

        Status oldStatus = incident.getStatus();
        incident.acknowledge(technician);
        incident.addAuditEntry(new AuditLogEntry(technicianEmail, "ACKNOWLEDGED",
                oldStatus.name(), Status.ACKNOWLEDGED.name(), "Incident acknowledged"));
        incidentRepository.save(incident);

        eventPublisher.publish(new StatusChangedEvent(
                incident.getId(), incident.getIncidentId(),
                oldStatus.name(), Status.ACKNOWLEDGED.name(), technicianEmail, LocalDateTime.now()
        ));

        return toIncidentResponse(incident);
    }

    @Transactional
    public IncidentResponse startInvestigation(Long incidentId, String technicianEmail) {
        Incident incident = findIncidentById(incidentId);
        Status oldStatus = incident.getStatus();
        incident.startInvestigation();
        incident.addAuditEntry(new AuditLogEntry(technicianEmail, "INVESTIGATION_STARTED",
                oldStatus.name(), Status.IN_PROGRESS.name(), "Investigation started"));
        incidentRepository.save(incident);

        eventPublisher.publish(new StatusChangedEvent(
                incident.getId(), incident.getIncidentId(),
                oldStatus.name(), Status.IN_PROGRESS.name(), technicianEmail, LocalDateTime.now()
        ));

        return toIncidentResponse(incident);
    }

    @Transactional
    public IncidentResponse resolveIncident(Long incidentId, ResolveIncidentRequest request, String technicianEmail) {
        Incident incident = findIncidentById(incidentId);
        Status oldStatus = incident.getStatus();
        incident.resolve(request.rootCause(), request.resolution(), request.preventiveMeasures());

        if (incident.getAssignedTechnician() != null) {
            incident.getAssignedTechnician().decrementWorkload();
            technicianRepository.save(incident.getAssignedTechnician());
        }

        incident.addAuditEntry(new AuditLogEntry(technicianEmail, "RESOLVED",
                oldStatus.name(), Status.RESOLVED.name(), "Resolution: " + request.resolution()));
        incidentRepository.save(incident);

        eventPublisher.publish(new IncidentResolvedEvent(
                incident.getId(), incident.getIncidentId(),
                technicianEmail, request.resolution(), LocalDateTime.now()
        ));

        return toIncidentResponse(incident);
    }

    @Transactional
    public IncidentResponse verifyIncident(Long incidentId, VerifyIncidentRequest request, String verifierEmail) {
        Incident incident = findIncidentById(incidentId);
        Status oldStatus = incident.getStatus();
        incident.verify(request.feedback(), request.rating());
        incident.addAuditEntry(new AuditLogEntry(verifierEmail, "VERIFIED",
                oldStatus.name(), Status.VERIFIED.name(),
                "Verification feedback: " + request.feedback() + " (Rating: " + request.rating() + ")"));
        incidentRepository.save(incident);

        eventPublisher.publish(new StatusChangedEvent(
                incident.getId(), incident.getIncidentId(),
                oldStatus.name(), Status.VERIFIED.name(), verifierEmail, LocalDateTime.now()
        ));

        // Auto-close after verification
        Status verifiedStatus = incident.getStatus();
        incident.close();
        incident.addAuditEntry(new AuditLogEntry("system@iitsm.org", "CLOSED",
                verifiedStatus.name(), Status.CLOSED.name(), "Auto-closed after verification"));
        incidentRepository.save(incident);

        eventPublisher.publish(new IncidentClosedEvent(
                incident.getId(), incident.getIncidentId(), LocalDateTime.now()
        ));

        return toIncidentResponse(incident);
    }

    @Transactional
    public IncidentResponse escalateIncident(Long incidentId, EscalateRequest request, String actorEmail) {
        Incident incident = findIncidentById(incidentId);
        EscalationLevel previousLevel = incident.getEscalationLevel();
        EscalationLevel newLevel = previousLevel.next();
        Status oldStatus = incident.getStatus();

        incident.escalate(newLevel);
        String reason = request != null && request.reason() != null ? request.reason() : "Manual escalation";
        incident.addAuditEntry(new AuditLogEntry(actorEmail, "ESCALATED",
                oldStatus.name(), Status.ESCALATED.name(),
                "Escalated from " + previousLevel + " to " + newLevel + ": " + reason));
        incidentRepository.save(incident);

        eventPublisher.publish(new IncidentEscalatedEvent(
                incident.getId(), incident.getIncidentId(),
                previousLevel.name(), newLevel.name(), reason, LocalDateTime.now()
        ));

        return toIncidentResponse(incident);
    }

    @Transactional
    public IncidentResponse addComment(Long incidentId, AddCommentRequest request, String authorEmail) {
        Incident incident = findIncidentById(incidentId);
        User author = employeeRepository.findByEmail(authorEmail)
                .map(e -> (User) e)
                .or(() -> technicianRepository.findByEmail(authorEmail).map(t -> (User) t))
                .orElseThrow(() -> new UserNotFoundException(authorEmail));

        Comment comment = new Comment(request.content(), author, request.internal());
        incident.addComment(comment);
        incidentRepository.save(incident);

        return toIncidentResponse(incident);
    }

    public IncidentResponse getIncident(Long id) {
        return toIncidentResponse(findIncidentById(id));
    }

    public IncidentResponse getIncidentByDisplayId(String displayId) {
        Incident incident = incidentRepository.findByIncidentId(displayId)
                .orElseThrow(() -> new IncidentNotFoundException(displayId));
        return toIncidentResponse(incident);
    }

    public List<IncidentSummaryResponse> getAllIncidents() {
        return incidentRepository.findAll().stream()
                .map(this::toSummary).collect(Collectors.toList());
    }

    public List<IncidentSummaryResponse> getActiveIncidents() {
        return incidentRepository.findActiveIncidents().stream()
                .map(this::toSummary).collect(Collectors.toList());
    }

    public List<IncidentSummaryResponse> getIncidentsByReporter(String email) {
        Employee reporter = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return incidentRepository.findByReporterId(reporter.getId()).stream()
                .map(this::toSummary).collect(Collectors.toList());
    }

    public List<IncidentSummaryResponse> getIncidentsByTechnician(String email) {
        Technician tech = technicianRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return incidentRepository.findByAssignedTechnicianId(tech.getId()).stream()
                .map(this::toSummary).collect(Collectors.toList());
    }

    public List<SuggestedResolutionResponse> getSuggestedResolutions(Long incidentId) {
        Incident incident = findIncidentById(incidentId);
        String category = incident.getCategory() != null ? incident.getCategory().getName() : null;
        return aiAssistant.suggestResolutions(incident.getTitle(), incident.getDescription(), category)
                .stream()
                .map(s -> new SuggestedResolutionResponse(s.incidentId(), s.title(),
                        s.resolution(), s.rootCause(), s.similarityScore()))
                .collect(Collectors.toList());
    }

    public List<DuplicateCandidateResponse> detectDuplicates(Long incidentId) {
        Incident incident = findIncidentById(incidentId);
        List<Incident> active = incidentRepository.findActiveIncidents();

        String queryText = (incident.getTitle() + " " +
                (incident.getDescription() != null ? incident.getDescription() : "")).toLowerCase();
        Set<String> queryTokens = tokenize(queryText);

        List<DuplicateCandidateResponse> candidates = new ArrayList<>();
        for (Incident other : active) {
            if (other.getId().equals(incident.getId())) continue;
            String otherText = (other.getTitle() + " " +
                    (other.getDescription() != null ? other.getDescription() : "")).toLowerCase();
            Set<String> otherTokens = tokenize(otherText);
            double similarity = jaccardSimilarity(queryTokens, otherTokens);
            if (similarity > 0.3) {
                candidates.add(new DuplicateCandidateResponse(
                        other.getIncidentId(), other.getTitle(), similarity));
            }
        }
        candidates.sort(Comparator.comparingDouble(DuplicateCandidateResponse::similarityScore).reversed());
        return candidates;
    }

    // ── Helper Methods ──

    private Incident findIncidentById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException(id.toString()));
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.split("\\W+"))
                .filter(s -> s.length() > 2)
                .collect(Collectors.toSet());
    }

    private double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    @Transactional
    public IncidentResponse updateDeadline(Long incidentId, UpdateDeadlineRequest request, String adminEmail) {
        Incident incident = findIncidentById(incidentId);
        LocalDateTime resolutionDeadline = request.resolutionDeadline();
        LocalDateTime responseDeadline = request.responseDeadline();

        if (request.addMinutes() != null && request.addMinutes() > 0) {
            LocalDateTime base = incident.getResolutionDeadline() != null ? incident.getResolutionDeadline() : LocalDateTime.now();
            resolutionDeadline = base.plusMinutes(request.addMinutes());
        }

        incident.setCustomDeadline(resolutionDeadline, responseDeadline, request.reason(), adminEmail);
        incidentRepository.save(incident);

        log.info("Admin {} updated deadline for incident {}", adminEmail, incident.getIncidentId());
        return toIncidentResponse(incident);
    }

    @Transactional
    public IncidentResponse reopenIncident(Long incidentId, String reason, String actorEmail) {
        Incident incident = findIncidentById(incidentId);
        incident.reopen(reason, actorEmail);
        incidentRepository.save(incident);
        log.info("Incident {} reopened by {}. Reason: {}", incident.getIncidentId(), actorEmail, reason);
        return toIncidentResponse(incident);
    }

    @Transactional
    public IncidentResponse requestIncidentInfo(Long incidentId, String question, String technicianEmail) {
        Incident incident = findIncidentById(incidentId);
        incident.requestInfo(question, technicianEmail);
        User author = technicianRepository.findByEmail(technicianEmail)
                .map(t -> (User) t)
                .or(() -> employeeRepository.findByEmail(technicianEmail).map(e -> (User) e))
                .orElse(null);
        if (author != null) {
            incident.addComment(new Comment("❓ [SUPPORT REQUEST] " + question, author, false));
        }
        incidentRepository.save(incident);
        log.info("Incident {} info requested by {}: {}", incident.getIncidentId(), technicianEmail, question);
        return toIncidentResponse(incident);
    }

    @Transactional
    public IncidentResponse resumeIncident(Long incidentId, String reason, String actorEmail) {
        Incident incident = findIncidentById(incidentId);
        incident.resumeWork(actorEmail, reason);
        incidentRepository.save(incident);
        log.info("Incident {} work resumed by {}: {}", incident.getIncidentId(), actorEmail, reason);
        return toIncidentResponse(incident);
    }

    @Transactional
    public IncidentResponse reassignIncident(Long id, Long technicianId, String actorEmail) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found: " + id));
        Technician newTech = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new com.iitsm.domain.exception.UserNotFoundException("Technician not found: " + technicianId));
        incident.reassign(newTech);
        incidentRepository.save(incident);
        log.info("Incident {} reassigned to {} by {}", incident.getIncidentId(), newTech.getEmail(), actorEmail);
        return toIncidentResponse(incident);
    }

    public IncidentResponse toIncidentResponse(Incident i) {
        List<AuditLogResponse> auditTrail = i.getAuditTrail().stream()
                .map(a -> new AuditLogResponse(a.getId(), a.getTimestamp(), a.getActorEmail(),
                        a.getAction(), a.getPreviousState(), a.getNewState(), a.getDescription()))
                .collect(Collectors.toList());

        List<CommentResponse> comments = i.getComments().stream()
                .map(c -> new CommentResponse(c.getId(), c.getContent(), c.getAuthor().getEmail(),
                        c.getAuthor().getFullName(), c.isInternal(), c.getCreatedAt()))
                .collect(Collectors.toList());

        return new IncidentResponse(
                i.getId(), i.getIncidentId(), i.getTitle(), i.getDescription(),
                i.getCategory() != null ? i.getCategory().getName() : null,
                i.getAffectedService() != null ? i.getAffectedService().getName() : null,
                i.getImpact() != null ? i.getImpact().name() : null,
                i.getUrgency() != null ? i.getUrgency().name() : null,
                i.getAffectedUsersCount(),
                i.getPriorityScore() != null ? i.getPriorityScore().getScore() : 0,
                i.getPriorityScore() != null ? i.getPriorityScore().getExplanation() : null,
                i.getCalculatedPriority() != null ? i.getCalculatedPriority().name() : null,
                i.getStatus().name(),
                i.getReporter() != null ? i.getReporter().getEmail() : null,
                i.getReporter() != null ? i.getReporter().getFullName() : null,
                i.getAssignedTechnician() != null ? i.getAssignedTechnician().getEmail() : null,
                i.getAssignedTechnician() != null ? i.getAssignedTechnician().getFullName() : null,
                i.getResponseDeadline(), i.getResolutionDeadline(),
                i.getResponseSlaStatus() != null ? i.getResponseSlaStatus().name() : null,
                i.getResolutionSlaStatus() != null ? i.getResolutionSlaStatus().name() : null,
                i.getRespondedAt(), i.getEscalationLevel() != null ? i.getEscalationLevel().name() : null,
                i.getRootCause(), i.getResolution(), i.getPreventiveMeasures(),
                i.getResolvedAt(), i.getVerificationFeedback(), i.getSatisfactionRating(),
                i.getCreatedAt(), i.getClosedAt(),
                auditTrail, comments,
                i.getAttachmentName(), i.getAttachmentType(), i.getAttachmentData(),
                i.getReopenCount()
        );
    }

    private IncidentSummaryResponse toSummary(Incident i) {
        return new IncidentSummaryResponse(
                i.getId(), i.getIncidentId(), i.getTitle(),
                i.getCalculatedPriority() != null ? i.getCalculatedPriority().name() : null,
                i.getStatus().name(),
                i.getCategory() != null ? i.getCategory().getName() : null,
                i.getAssignedTechnician() != null ? i.getAssignedTechnician().getFullName() : null,
                i.getResponseSlaStatus() != null ? i.getResponseSlaStatus().name() : null,
                i.getResolutionSlaStatus() != null ? i.getResolutionSlaStatus().name() : null,
                i.getCreatedAt()
        );
    }
}
