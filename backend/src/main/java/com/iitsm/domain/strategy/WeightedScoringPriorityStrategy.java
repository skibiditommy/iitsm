package com.iitsm.domain.strategy;

import com.iitsm.domain.valueobject.PriorityScore;
import org.springframework.stereotype.Component;

/**
 * Weighted-scoring priority calculation strategy.
 * Computes a composite score from impact, urgency, service criticality,
 * and an affected-user factor, each weighted to reflect organisational priorities.
 */
@Component
public class WeightedScoringPriorityStrategy implements PriorityCalculationStrategy {

    private static final String STRATEGY_NAME = "weighted-scoring";

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public PriorityScore calculate(int impactValue, int urgencyValue, int serviceCriticality, int affectedUsersCount) {
        double userCountFactor = Math.min(affectedUsersCount / 100.0, 1.0) * 4.0;

        double score = 0.30 * impactValue
                     + 0.30 * urgencyValue
                     + 0.25 * serviceCriticality
                     + 0.15 * userCountFactor;

        String priority;
        if (score >= 3.2) {
            priority = "P1";
        } else if (score >= 2.4) {
            priority = "P2";
        } else if (score >= 1.6) {
            priority = "P3";
        } else {
            priority = "P4";
        }

        String explanation = String.format(
                "Weighted Score: %.2f (Impact:%d×0.30 + Urgency:%d×0.30 + Criticality:%d×0.25 + UserFactor:%.1f×0.15) → %s",
                score, impactValue, urgencyValue, serviceCriticality, userCountFactor, priority
        );

        return new PriorityScore(score, explanation, priority);
    }
}
