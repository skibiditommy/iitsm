package com.iitsm.domain.strategy;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.Technician;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Skill-based technician assignment strategy.
 * Filters technicians who possess the required skill for the incident's category,
 * then selects the one with the lowest current workload.
 */
@Component
public class SkillBasedAssignmentStrategy implements TechnicianAssignmentStrategy {

    private static final String STRATEGY_NAME = "skill-based";

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public Optional<Technician> assignTechnician(Incident incident, List<Technician> availableTechnicians) {
        if (availableTechnicians == null || availableTechnicians.isEmpty()) {
            return Optional.empty();
        }

        return availableTechnicians.stream()
                .filter(tech -> tech.hasSkillFor(incident.getCategory()))
                .min(Comparator.comparingInt(Technician::getCurrentWorkload));
    }
}
