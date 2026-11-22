package com.iitsm.domain.strategy;

import com.iitsm.domain.valueobject.PriorityScore;
import org.springframework.stereotype.Component;

/**
 * Matrix-rule-based priority calculation strategy following the standard ITIL priority matrix.
 * Uses a 2D lookup table indexed by impact and urgency, with an optional
 * service-criticality escalation that promotes the priority by one level.
 */
@Component
public class MatrixRuleBasedPriorityStrategy implements PriorityCalculationStrategy {

    private static final String STRATEGY_NAME = "matrix-rule-based";

    /*
     * ITIL priority matrix (impact rows × urgency columns).
     * Indices: 0 = LOW(1), 1 = MEDIUM(2), 2 = HIGH(3), 3 = CRITICAL(4).
     *
     *              Urgency →   1(LOW)  2(MED)  3(HIGH)  4(CRIT)
     * Impact ↓
     *   1 (LOW)                 P4      P3      P3       P3
     *   2 (MEDIUM)              P3      P3      P2       P2
     *   3 (HIGH)                P3      P2      P2       P1
     *   4 (CRITICAL)            P3      P2      P1       P1
     */
    private static final String[][] MATRIX = {
            // urgency:  1(LOW)  2(MED)  3(HIGH) 4(CRIT)
            /* impact 1 (LOW)      */ {"P4", "P3", "P3", "P3"},
            /* impact 2 (MEDIUM)   */ {"P3", "P3", "P2", "P2"},
            /* impact 3 (HIGH)     */ {"P3", "P2", "P2", "P1"},
            /* impact 4 (CRITICAL) */ {"P3", "P2", "P1", "P1"},
    };

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public PriorityScore calculate(int impactValue, int urgencyValue, int serviceCriticality, int affectedUsersCount) {
        // Clamp values to [1,4] to safely index the matrix
        int impactIdx = clamp(impactValue, 1, 4) - 1;
        int urgencyIdx = clamp(urgencyValue, 1, 4) - 1;

        String basePriority = MATRIX[impactIdx][urgencyIdx];

        // If serviceCriticality >= 4, escalate one level
        String finalPriority = basePriority;
        boolean escalated = false;
        if (serviceCriticality >= 4) {
            finalPriority = escalateOneLevel(basePriority);
            escalated = !finalPriority.equals(basePriority);
        }

        double score = (double) impactValue * urgencyValue;

        String explanation;
        if (escalated) {
            explanation = String.format(
                    "Matrix Lookup: Impact=%d × Urgency=%d → base %s, serviceCriticality=%d (≥4) escalated → %s (score=%.1f)",
                    impactValue, urgencyValue, basePriority, serviceCriticality, finalPriority, score
            );
        } else {
            explanation = String.format(
                    "Matrix Lookup: Impact=%d × Urgency=%d → %s (serviceCriticality=%d, score=%.1f)",
                    impactValue, urgencyValue, finalPriority, serviceCriticality, score
            );
        }

        return new PriorityScore(score, explanation, finalPriority);
    }

    /**
     * Escalates a priority label by one level (e.g. P2→P1, P3→P2, P4→P3). P1 stays P1.
     */
    private String escalateOneLevel(String priority) {
        return switch (priority) {
            case "P2" -> "P1";
            case "P3" -> "P2";
            case "P4" -> "P3";
            default -> priority; // P1 cannot be escalated further
        };
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
