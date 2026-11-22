package com.iitsm.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable record of a state change or significant action taken on an {@link Incident}.
 * <p>
 * OOP: Composition — AuditLogEntry exists only as part of an Incident's audit trail.
 * It cannot exist independently; deleting the parent Incident cascades deletion
 * to all associated audit entries.
 * </p>
 */
@Entity
@Table(name = "audit_log_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String actorEmail;

    @Column(nullable = false)
    private String action;

    private String previousState;

    private String newState;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    @JsonIgnore
    private Incident incident;

    public AuditLogEntry(String actorEmail, String action, String previousState,
                         String newState, String description) {
        this.actorEmail = actorEmail;
        this.action = action;
        this.previousState = previousState;
        this.newState = newState;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Sets the parent incident. Package-private — called by {@link Incident#addAuditEntry(AuditLogEntry)}.
     */
    void setIncident(Incident incident) {
        this.incident = incident;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLogEntry that = (AuditLogEntry) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "AuditLogEntry{id=" + id + ", action='" + action + "', timestamp=" + timestamp + "}";
    }
}
