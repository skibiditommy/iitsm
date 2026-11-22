package com.iitsm.domain.model;

import com.iitsm.domain.enums.Priority;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Defines SLA (Service Level Agreement) response and resolution time targets
 * for a given priority level, optionally scoped to a specific {@link ITService}.
 * <p>
 * The unique constraint on (priority, service_id) ensures at most one policy
 * per priority/service combination.
 * </p>
 */
@Entity
@Table(name = "sla_policies", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"priority", "service_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ITService service;

    private long responseMinutes;

    private long resolutionMinutes;

    public SlaPolicy(Priority priority, ITService service, long responseMinutes, long resolutionMinutes) {
        this.priority = priority;
        this.service = service;
        this.responseMinutes = responseMinutes;
        this.resolutionMinutes = resolutionMinutes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SlaPolicy that = (SlaPolicy) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "SlaPolicy{id=" + id + ", priority=" + priority
                + ", response=" + responseMinutes + "min, resolution=" + resolutionMinutes + "min}";
    }
}
