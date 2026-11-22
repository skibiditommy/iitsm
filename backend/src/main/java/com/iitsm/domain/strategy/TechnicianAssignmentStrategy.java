package com.iitsm.domain.strategy;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.Technician;

import java.util.List;
import java.util.Optional;

/**
 * Strategy Pattern: Defines a pluggable algorithm for selecting a Technician
 * to handle an Incident. Implementations include skill-based, load-based,
 * round-robin, and smart-composite approaches.
 */
public interface TechnicianAssignmentStrategy {

    /**
     * @return a unique name identifying this strategy (e.g. "skill-based")
     */
    String getStrategyName();

    /**
     * Selects the best-fit technician for the given incident from the available pool.
     *
     * @param incident             the incident requiring assignment
     * @param availableTechnicians the pool of technicians to choose from
     * @return an Optional containing the selected Technician, or empty if none qualifies
     */
    Optional<Technician> assignTechnician(Incident incident, List<Technician> availableTechnicians);
}
