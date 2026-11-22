package com.iitsm.domain.strategy;

import com.iitsm.domain.valueobject.PriorityScore;

/**
 * Strategy Pattern: Defines a pluggable algorithm for computing incident priority.
 * Multiple implementations can coexist (weighted scoring, matrix-based, etc.)
 * and be swapped at runtime without changing the calling code.
 */
public interface PriorityCalculationStrategy {

    /**
     * @return a unique name identifying this strategy (e.g. "weighted-scoring")
     */
    String getStrategyName();

    /**
     * Calculates a priority score from the given incident attributes.
     *
     * @param impactValue        impact level (1-4)
     * @param urgencyValue       urgency level (1-4)
     * @param serviceCriticality criticality of the affected service (1-4)
     * @param affectedUsersCount number of users affected
     * @return a PriorityScore containing the numeric score, explanation, and priority label
     */
    PriorityScore calculate(int impactValue, int urgencyValue, int serviceCriticality, int affectedUsersCount);
}
