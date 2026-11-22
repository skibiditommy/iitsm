package com.iitsm.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Represents a technical skill that a {@link Technician} can possess.
 * <p>
 * OOP: Aggregation — Skills exist independently of Technicians.
 * A Skill is optionally related to an {@link IncidentCategory}, enabling
 * skill-based routing of incidents to qualified technicians.
 * </p>
 */
@Entity
@Table(name = "skills")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_category_id")
    private IncidentCategory relatedCategory;

    public Skill(String name, IncidentCategory relatedCategory) {
        this.name = name;
        this.relatedCategory = relatedCategory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Skill skill = (Skill) o;
        return id != null && Objects.equals(id, skill.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Skill{id=" + id + ", name='" + name + "'}";
    }
}
