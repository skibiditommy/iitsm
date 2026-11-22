package com.iitsm.domain.model;

import com.iitsm.domain.enums.*;
import com.iitsm.domain.exception.InvalidStateTransitionException;
import com.iitsm.domain.valueobject.PriorityScore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * The core domain entity representing an IT incident throughout its full lifecycle.
 * <p>
 * OOP: Composition (owns {@link AuditLogEntry AuditLogEntries} and resolution details),
 * Association ({@link Employee} reporter, {@link Technician} assignee, {@link ITService},
 * {@link IncidentCategory}). Encapsulation — all business rules and state transitions
 * are enforced through domain methods; no public setters exist for status,
 * calculatedPriority, or any other business-critical field.
 * </p>
 *
 * <h3>Lifecycle States</h3>
 * <pre>
 * NEW → CLASSIFIED → ASSIGNED → ACKNOWLEDGED → IN_PROGRESS → RESOLVED → VERIFIED → CLOSED
 *                                                                ↑
 *                                   ESCALATED ←─────────────────┘
 * </pre>
 */
@Entity
@Table(name = "incidents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String incidentId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private IncidentCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affected_service_id")
    private ITService affectedService;

    @Enumerated(EnumType.STRING)
    private Impact impact;

    @Enumerated(EnumType.STRING)
    private Urgency urgency;

    private int affectedUsersCount;

    @Embedded
    private PriorityScore priorityScore;

    @Enumerated(EnumType.STRING)
    private Priority calculatedPriority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private Employee reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_technician_id")
    private Technician assignedTechnician;

    private LocalDateTime responseDeadline;

    private LocalDateTime resolutionDeadline;

    @Enumerated(EnumType.STRING)
    private SlaStatus responseSlaStatus = SlaStatus.ON_TRACK;

    @Enumerated(EnumType.STRING)
    private SlaStatus resolutionSlaStatus = SlaStatus.ON_TRACK;

    private LocalDateTime respondedAt;

    @Enumerated(EnumType.STRING)
    private EscalationLevel escalationLevel = EscalationLevel.TECHNICIAN;

    private String rootCause;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    private String preventiveMeasures;

    private LocalDateTime resolvedAt;

    private String verificationFeedback;

    private Integer satisfactionRating;

    @Column(columnDefinition = "TEXT")
    private String attachmentData;

    private String attachmentName;

    private String attachmentType;

    @Column(name = "reopen_count", nullable = false)
    private int reopenCount = 0;

    @Column(name = "sla_paused_at")
    private LocalDateTime slaPausedAt;

    @Column(name = "total_sla_paused_minutes")
    private Long totalSlaPausedMinutes = 0L;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp ASC")
    private List<AuditLogEntry> auditTrail = new ArrayList<>();

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Comment> comments = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime closedAt;

    // ──────────────────────────────────────────────────────────────
    // Constructor
    // ──────────────────────────────────────────────────────────────

    /**
     * Creates a new incident with all required initial data.
     *
     * @param incidentId         unique human-readable identifier (e.g., "INC-1001")
     * @param title              short summary of the incident
     * @param description        detailed description
     * @param reporter           the employee who reported the incident
     * @param category           incident category
     * @param affectedService    the IT service affected
     * @param impact             business impact level
     * @param urgency            urgency level
     * @param affectedUsersCount number of users affected
     * @param priorityScore      AI-computed priority score
     * @param calculatedPriority resulting priority enum
     * @param responseDeadline   SLA response deadline
     * @param resolutionDeadline SLA resolution deadline
     */
    public Incident(String incidentId, String title, String description,
                    Employee reporter, IncidentCategory category, ITService affectedService,
                    Impact impact, Urgency urgency, int affectedUsersCount,
                    PriorityScore priorityScore, Priority calculatedPriority,
                    LocalDateTime responseDeadline, LocalDateTime resolutionDeadline) {
        this.incidentId = incidentId;
        this.title = title;
        this.description = description;
        this.reporter = reporter;
        this.category = category;
        this.affectedService = affectedService;
        this.impact = impact;
        this.urgency = urgency;
        this.affectedUsersCount = affectedUsersCount;
        this.priorityScore = priorityScore;
        this.calculatedPriority = calculatedPriority;
        this.responseDeadline = responseDeadline;
        this.resolutionDeadline = resolutionDeadline;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ──────────────────────────────────────────────────────────────
    // Domain Methods — State Transitions
    // ──────────────────────────────────────────────────────────────

    /**
     * Classifies the incident by assigning a category and affected service.
     * Only allowed from {@link Status#NEW}.
     *
     * @param category the incident category
     * @param service  the affected IT service
     * @throws InvalidStateTransitionException if not in NEW status
     */
    public void classify(IncidentCategory category, ITService service) {
        if (status != Status.NEW) {
            throw new InvalidStateTransitionException(status.name(), Status.CLASSIFIED.name(),
                    "Incident can only be classified from NEW status");
        }
        this.category = category;
        this.affectedService = service;
        this.status = Status.CLASSIFIED;
    }

    /**
     * Assigns a technician to handle this incident.
     * Only allowed from {@link Status#CLASSIFIED} or {@link Status#ESCALATED}.
     *
     * @param technician the technician to assign
     * @throws InvalidStateTransitionException if not in an assignable status
     */
    public void assignTechnician(Technician technician) {
        if (status != Status.CLASSIFIED && status != Status.ESCALATED) {
            throw new InvalidStateTransitionException(status.name(), Status.ASSIGNED.name(),
                    "Incident can only be assigned from CLASSIFIED or ESCALATED status");
        }
        this.assignedTechnician = technician;
        this.status = Status.ASSIGNED;
        technician.incrementWorkload();
    }

    /**
     * Acknowledges the incident by the assigned technician.
     * Only allowed from {@link Status#ASSIGNED} and only by the assigned technician.
     *
     * @param technician the technician acknowledging
     * @throws InvalidStateTransitionException if not in ASSIGNED status or technician mismatch
     */
    public void acknowledge(Technician technician) {
        if (status != Status.ASSIGNED) {
            throw new InvalidStateTransitionException(status.name(), Status.ACKNOWLEDGED.name(),
                    "Incident can only be acknowledged from ASSIGNED status");
        }
        boolean sameTechnician = false;
        if (assignedTechnician != null && technician != null) {
            if (assignedTechnician.getId() != null && technician.getId() != null) {
                sameTechnician = assignedTechnician.getId().equals(technician.getId());
            } else {
                sameTechnician = (assignedTechnician == technician) || Objects.equals(assignedTechnician.getEmail(), technician.getEmail());
            }
        }
        if (assignedTechnician == null || !sameTechnician) {
            throw new InvalidStateTransitionException(status.name(), Status.ACKNOWLEDGED.name(),
                    "Only the assigned technician can acknowledge this incident");
        }
        this.status = Status.ACKNOWLEDGED;
        this.respondedAt = LocalDateTime.now();
    }

    /**
     * Transitions the incident to active investigation.
     * Only allowed from {@link Status#ACKNOWLEDGED}.
     *
     * @throws InvalidStateTransitionException if not in ACKNOWLEDGED status
     */
    public void startInvestigation() {
        if (status != Status.ACKNOWLEDGED) {
            throw new InvalidStateTransitionException(status.name(), Status.IN_PROGRESS.name(),
                    "Investigation can only start from ACKNOWLEDGED status");
        }
        this.status = Status.IN_PROGRESS;
    }

    /**
     * Resolves the incident with root cause analysis and resolution details.
     * Only allowed from {@link Status#IN_PROGRESS}.
     *
     * @param rootCause          identified root cause
     * @param resolution         description of how the incident was resolved
     * @param preventiveMeasures steps taken to prevent recurrence
     * @throws InvalidStateTransitionException if not in IN_PROGRESS status
     */
    public void resolve(String rootCause, String resolution, String preventiveMeasures) {
        if (status != Status.IN_PROGRESS) {
            throw new InvalidStateTransitionException(status.name(), Status.RESOLVED.name(),
                    "Incident can only be resolved from IN_PROGRESS status");
        }
        this.rootCause = rootCause;
        this.resolution = resolution;
        this.preventiveMeasures = preventiveMeasures;
        this.status = Status.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * Verifies the resolution of the incident (typically by the reporter).
     * Only allowed from {@link Status#RESOLVED}.
     *
     * @param feedback user feedback on the resolution
     * @param rating   satisfaction rating from 1 to 5
     * @throws InvalidStateTransitionException if not in RESOLVED status
     * @throws IllegalArgumentException        if rating is not between 1 and 5
     */
    public void verify(String feedback, int rating) {
        if (status != Status.RESOLVED) {
            throw new InvalidStateTransitionException(status.name(), Status.VERIFIED.name(),
                    "Incident can only be verified from RESOLVED status");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Satisfaction rating must be between 1 and 5, got: " + rating);
        }
        this.verificationFeedback = feedback;
        this.satisfactionRating = rating;
        this.status = Status.VERIFIED;
    }

    /**
     * Closes the incident after verification.
     * Only allowed from {@link Status#VERIFIED}.
     *
     * @throws InvalidStateTransitionException if not in VERIFIED status
     */
    public void close() {
        if (status != Status.VERIFIED) {
            throw new InvalidStateTransitionException(status.name(), Status.CLOSED.name(),
                    "Incident can only be closed from VERIFIED status");
        }
        this.status = Status.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    /**
     * Reopens a resolved incident when the reporter rejects the resolution.
     * Transitions from RESOLVED back to IN_PROGRESS and increments reopenCount.
     */
    public void reopen(String reason, String actorEmail) {
        if (status != Status.RESOLVED) {
            throw new InvalidStateTransitionException(status.name(), Status.IN_PROGRESS.name(),
                    "Incident can only be reopened from RESOLVED status");
        }
        Status oldStatus = this.status;
        this.status = Status.IN_PROGRESS;
        this.reopenCount++;
        this.resolvedAt = null;
        this.verificationFeedback = "Resolution rejected: " + reason;
        addAuditEntry(new AuditLogEntry(
                actorEmail != null ? actorEmail : "reporter@iitsm.org",
                "INCIDENT_REOPENED", oldStatus.name(), Status.IN_PROGRESS.name(),
                "Resolution rejected by employee. Reason: " + reason + " (Reopen count: " + reopenCount + ")"
        ));
    }

    /**
     * Technician requests additional clarification from the reporting employee.
     * Transitions incident to PENDING_USER state and PAUSES the SLA resolution clock.
     */
    public void requestInfo(String question, String technicianEmail) {
        if (status != Status.IN_PROGRESS && status != Status.ACKNOWLEDGED) {
            throw new InvalidStateTransitionException(status.name(), Status.PENDING_USER.name(),
                    "Can only request additional info during investigation");
        }
        Status oldStatus = this.status;
        this.status = Status.PENDING_USER;
        this.slaPausedAt = LocalDateTime.now();
        this.resolutionSlaStatus = SlaStatus.PAUSED;
        addAuditEntry(new AuditLogEntry(
                technicianEmail != null ? technicianEmail : "technician@iitsm.org",
                "INFO_REQUESTED", oldStatus.name(), Status.PENDING_USER.name(),
                "Technician requested additional info: " + question + ". SLA clock PAUSED in PENDING_USER state."
        ));
    }

    /**
     * Reporter provides response or technician resumes work.
     * Transitions incident from PENDING_USER back to IN_PROGRESS,
     * calculates the paused duration, and extends the resolution deadline accordingly.
     */
    public void resumeWork(String actorEmail, String reason) {
        if (this.status != Status.PENDING_USER) {
            return; // Only applies when currently in PENDING_USER state
        }
        Status oldStatus = this.status;
        this.status = Status.IN_PROGRESS;
        long pausedMinutes = 0L;
        if (this.slaPausedAt != null) {
            pausedMinutes = Math.max(1, Duration.between(this.slaPausedAt, LocalDateTime.now()).toMinutes());
            this.totalSlaPausedMinutes = (this.totalSlaPausedMinutes != null ? this.totalSlaPausedMinutes : 0L) + pausedMinutes;
            if (this.resolutionDeadline != null) {
                this.resolutionDeadline = this.resolutionDeadline.plusMinutes(pausedMinutes);
            }
            this.slaPausedAt = null;
        }
        this.resolutionSlaStatus = SlaStatus.ON_TRACK;
        addAuditEntry(new AuditLogEntry(
                actorEmail != null ? actorEmail : "reporter@iitsm.org",
                "WORK_RESUMED", oldStatus.name(), Status.IN_PROGRESS.name(),
                "Work resumed: " + (reason != null ? reason : "Information provided") +
                ". SLA clock resumed; resolution deadline extended by " + pausedMinutes + " minutes."
        ));
    }

    public LocalDateTime getSlaPausedAt() {
        return slaPausedAt;
    }

    public Long getTotalSlaPausedMinutes() {
        return totalSlaPausedMinutes;
    }

    public int getReopenCount() {
        return reopenCount;
    }

    /**
     * Escalates the incident to a higher support level.
     * Allowed from NEW, CLASSIFIED, ASSIGNED, ACKNOWLEDGED, or IN_PROGRESS.
     *
     * @param newLevel the new escalation level
     * @throws InvalidStateTransitionException if in a terminal or non-escalatable status
     */
    public void escalate(EscalationLevel newLevel) {
        Set<Status> escalatableStatuses = EnumSet.of(
                Status.NEW, Status.CLASSIFIED, Status.ASSIGNED,
                Status.ACKNOWLEDGED, Status.IN_PROGRESS, Status.ESCALATED
        );
        if (!escalatableStatuses.contains(status)) {
            throw new InvalidStateTransitionException(status.name(), Status.ESCALATED.name(),
                    "Incident cannot be escalated from " + status + " status");
        }
        this.escalationLevel = newLevel;
        this.status = Status.ESCALATED;
    }

    /**
     * Reassigns the incident to a different technician.
     * Allowed from ASSIGNED, ACKNOWLEDGED, IN_PROGRESS, or ESCALATED.
     * Decrements the old technician's workload and increments the new one's.
     *
     * @param newTechnician the new technician to assign
     * @throws InvalidStateTransitionException if not in a reassignable status
     */
    public void reassign(Technician newTechnician) {
        Set<Status> reassignableStatuses = EnumSet.of(
                Status.ASSIGNED, Status.ACKNOWLEDGED,
                Status.IN_PROGRESS, Status.ESCALATED
        );
        if (!reassignableStatuses.contains(status)) {
            throw new InvalidStateTransitionException(status.name(), Status.ASSIGNED.name(),
                    "Incident cannot be reassigned from " + status + " status");
        }
        if (assignedTechnician != null) {
            assignedTechnician.decrementWorkload();
        }
        this.assignedTechnician = newTechnician;
        newTechnician.incrementWorkload();
    }

    // ──────────────────────────────────────────────────────────────
    // Collection Management
    // ──────────────────────────────────────────────────────────────

    /**
     * Adds an audit log entry to this incident's audit trail.
     *
     * @param entry the audit entry to add
     */
    public void addAuditEntry(AuditLogEntry entry) {
        auditTrail.add(entry);
        entry.setIncident(this);
    }

    /**
     * Adds a comment to this incident.
     *
     * @param comment the comment to add
     */
    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setIncident(this);
        if (this.status == Status.PENDING_USER) {
            String authorEmail = comment.getAuthor() != null ? comment.getAuthor().getEmail() : "reporter@iitsm.org";
            boolean isSupportQuestion = comment.getContent() != null && comment.getContent().startsWith("❓ [SUPPORT REQUEST]");
            if (!isSupportQuestion) {
                resumeWork(authorEmail, "User response received: " + comment.getContent());
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // SLA Management
    // ──────────────────────────────────────────────────────────────

    /**
     * Updates the SLA status for both response and resolution tracking.
     *
     * @param responseSla  the new response SLA status
     * @param resolutionSla the new resolution SLA status
     */
    public void updateSlaStatus(SlaStatus responseSla, SlaStatus resolutionSla) {
        this.responseSlaStatus = responseSla;
        this.resolutionSlaStatus = resolutionSla;
    }

    /**
     * Checks whether either SLA (response or resolution) has been breached.
     *
     * @return {@code true} if any SLA is breached
     */
    public boolean isSlaBreached() {
        return responseSlaStatus == SlaStatus.BREACHED || resolutionSlaStatus == SlaStatus.BREACHED;
    }

    public void setAttachment(String name, String type, String data) {
        this.attachmentName = name;
        this.attachmentType = type;
        this.attachmentData = data;
    }

    /**
     * Admin sets a custom resolution and/or response deadline for the assigned technician.
     * Enforces audit trail logging for traceability.
     */
    public void setCustomDeadline(LocalDateTime customResolutionDeadline, LocalDateTime customResponseDeadline, String reason, String actorEmail) {
        if (customResolutionDeadline != null) {
            this.resolutionDeadline = customResolutionDeadline;
        }
        if (customResponseDeadline != null) {
            this.responseDeadline = customResponseDeadline;
        }
        addAuditEntry(new AuditLogEntry(
                actorEmail,
                "DEADLINE_UPDATED",
                status.name(),
                status.name(),
                "Admin updated incident target deadline. " + (reason != null && !reason.isBlank() ? "Reason: " + reason : "")
        ));
    }

    // ──────────────────────────────────────────────────────────────
    // Status Queries
    // ──────────────────────────────────────────────────────────────

    /**
     * Checks whether the incident is still active (not closed, resolved, or verified).
     *
     * @return {@code true} if the incident is active
     */
    public boolean isActive() {
        return status != Status.CLOSED
                && status != Status.RESOLVED
                && status != Status.VERIFIED;
    }

    // ──────────────────────────────────────────────────────────────
    // Seed Data Support
    // ──────────────────────────────────────────────────────────────

    /**
     * Static factory method for creating incidents with arbitrary state during seed data loading.
     * <p>
     * <strong>WARNING:</strong> This method bypasses all domain validation and should ONLY
     * be used for database seeding, testing, and migration. Never use in production business logic.
     * </p>
     *
     * @return a fully populated Incident with all fields set directly
     */
    public static Incident forSeed(String incidentId, String title, String description,
                                   Employee reporter, IncidentCategory category,
                                   ITService affectedService, Impact impact, Urgency urgency,
                                   int affectedUsersCount, PriorityScore priorityScore,
                                   Priority calculatedPriority, Status status,
                                   Technician assignedTechnician,
                                   LocalDateTime responseDeadline, LocalDateTime resolutionDeadline,
                                   SlaStatus responseSlaStatus, SlaStatus resolutionSlaStatus,
                                   LocalDateTime respondedAt, EscalationLevel escalationLevel,
                                   String rootCause, String resolution, String preventiveMeasures,
                                   LocalDateTime resolvedAt, String verificationFeedback,
                                   Integer satisfactionRating, LocalDateTime closedAt) {
        Incident incident = new Incident();
        incident.incidentId = incidentId;
        incident.title = title;
        incident.description = description;
        incident.reporter = reporter;
        incident.category = category;
        incident.affectedService = affectedService;
        incident.impact = impact;
        incident.urgency = urgency;
        incident.affectedUsersCount = affectedUsersCount;
        incident.priorityScore = priorityScore;
        incident.calculatedPriority = calculatedPriority;
        incident.status = status;
        incident.assignedTechnician = assignedTechnician;
        incident.responseDeadline = responseDeadline;
        incident.resolutionDeadline = resolutionDeadline;
        incident.responseSlaStatus = responseSlaStatus != null ? responseSlaStatus : SlaStatus.ON_TRACK;
        incident.resolutionSlaStatus = resolutionSlaStatus != null ? resolutionSlaStatus : SlaStatus.ON_TRACK;
        incident.respondedAt = respondedAt;
        incident.escalationLevel = escalationLevel != null ? escalationLevel : EscalationLevel.TECHNICIAN;
        incident.rootCause = rootCause;
        incident.resolution = resolution;
        incident.preventiveMeasures = preventiveMeasures;
        incident.resolvedAt = resolvedAt;
        incident.verificationFeedback = verificationFeedback;
        incident.satisfactionRating = satisfactionRating;
        incident.closedAt = closedAt;
        incident.createdAt = LocalDateTime.now();
        return incident;
    }

    // ──────────────────────────────────────────────────────────────
    // Package-private setters for seed/test support
    // ──────────────────────────────────────────────────────────────

    void setStatus(Status status) {
        this.status = status;
    }

    void setAssignedTechnician(Technician technician) {
        this.assignedTechnician = technician;
    }

    void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    void setEscalationLevel(EscalationLevel level) {
        this.escalationLevel = level;
    }

    void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    void setResolution(String resolution) {
        this.resolution = resolution;
    }

    void setPreventiveMeasures(String preventiveMeasures) {
        this.preventiveMeasures = preventiveMeasures;
    }

    void setVerificationFeedback(String feedback) {
        this.verificationFeedback = feedback;
    }

    void setSatisfactionRating(Integer rating) {
        this.satisfactionRating = rating;
    }

    void setResponseSlaStatus(SlaStatus slaStatus) {
        this.responseSlaStatus = slaStatus;
    }

    void setResolutionSlaStatus(SlaStatus slaStatus) {
        this.resolutionSlaStatus = slaStatus;
    }

    // ──────────────────────────────────────────────────────────────
    // Object overrides
    // ──────────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Incident incident = (Incident) o;
        return id != null && Objects.equals(id, incident.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Incident{id=" + id + ", incidentId='" + incidentId
                + "', status=" + status + ", priority=" + calculatedPriority + "}";
    }
}
