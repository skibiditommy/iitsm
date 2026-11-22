package com.iitsm.domain.strategy;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.Technician;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Load-based technician assignment strategy.
 * Selects the technician with the lowest workload-to-capacity ratio.
 * Ties are broken by highest years of experience.
 */
@Component
public class LoadBasedAssignmentStrategy implements TechnicianAssignmentStrategy {

    private static final String STRATEGY_NAME = "load-based";

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
                .min(Comparator
                        .comparingDouble((Technician tech) ->
                                tech.getCurrentWorkload() / (double) tech.getMaxWorkload())
                        .thenComparing(
                                Comparator.comparingInt(Technician::getYearsOfExperience).reversed())
                );
    }
}
