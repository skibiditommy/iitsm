package com.iitsm.domain.model;

import com.iitsm.domain.enums.Action;
import com.iitsm.domain.enums.Permission;
import com.iitsm.domain.enums.Role;
import com.iitsm.domain.exception.TechnicianWorkloadExceededException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a technician who resolves incidents.
 * <p>
 * Technicians possess {@link Skill}s that determine which incident categories
 * they are qualified to handle. Workload management prevents overloading:
 * a technician cannot be assigned more incidents than their {@code maxWorkload}.
 * </p>
 */
@Entity
@DiscriminatorValue("TECHNICIAN")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Technician extends User {

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "technician_skills",
            joinColumns = @JoinColumn(name = "technician_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills = new HashSet<>();

    private int maxWorkload = 5;

    private int currentWorkload = 0;

    private int yearsOfExperience;

    private boolean available = true;

    public Technician(String email, String passwordHash, String firstName, String lastName,
                      int yearsOfExperience) {
        super(email, passwordHash, firstName, lastName, Role.TECHNICIAN);
        this.yearsOfExperience = yearsOfExperience;
    }

    /**
     * Checks whether this technician has a skill related to the given incident category.
     *
     * @param category the incident category to check against
     * @return {@code true} if the technician has a matching skill
     */
    public boolean hasSkillFor(IncidentCategory category) {
        if (category == null) {
            return false;
        }
        for (Skill skill : skills) {
            if (skill.getRelatedCategory() != null) {
                if (category.getId() != null && skill.getRelatedCategory().getId() != null) {
                    if (skill.getRelatedCategory().getId().equals(category.getId())) {
                        return true;
                    }
                } else if (Objects.equals(category.getName(), skill.getRelatedCategory().getName())
                        || skill.getRelatedCategory().equals(category)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Increments the technician's current workload by one.
     *
     * @throws TechnicianWorkloadExceededException if the technician is already at max workload
     */
    public void incrementWorkload() {
        if (currentWorkload >= maxWorkload) {
            throw new TechnicianWorkloadExceededException(getId(), currentWorkload, maxWorkload);
        }
        currentWorkload++;
    }

    /**
     * Decrements the technician's current workload by one (minimum 0).
     */
    public void decrementWorkload() {
        if (currentWorkload > 0) {
            currentWorkload--;
        }
    }

    /**
     * Checks whether the technician's current workload has reached the maximum.
     *
     * @return {@code true} if overloaded
     */
    public boolean isOverloaded() {
        return currentWorkload >= maxWorkload;
    }

    public void addSkill(Skill skill) {
        skills.add(skill);
    }

    public void removeSkill(Skill skill) {
        skills.remove(skill);
    }

    public void setMaxWorkload(int max) {
        this.maxWorkload = max;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setYearsOfExperience(int yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    @Override
    public Set<Permission> getDashboardPermissions() {
        return Set.of(
                Permission.VIEW_ASSIGNED_INCIDENTS,
                Permission.RESOLVE_INCIDENT,
                Permission.VIEW_OWN_INCIDENTS
        );
    }

    @Override
    public boolean canPerformAction(Action action) {
        return action == Action.ACKNOWLEDGE_INCIDENT
                || action == Action.RESOLVE_INCIDENT
                || action == Action.VIEW_INCIDENT;
    }
}
