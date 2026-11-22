package com.iitsm.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Represents a category for classifying incidents (e.g., "Network", "Hardware", "Software").
 * Used to match incidents with technicians who possess relevant skills.
 */
@Entity
@Table(name = "incident_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IncidentCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    public IncidentCategory(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IncidentCategory that = (IncidentCategory) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "IncidentCategory{id=" + id + ", name='" + name + "'}";
    }
}
