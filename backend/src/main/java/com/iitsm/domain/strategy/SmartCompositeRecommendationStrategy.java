package com.iitsm.domain.strategy;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.Technician;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Smart composite technician assignment strategy.
 * Evaluates every available technician against multiple weighted factors
 * (skill match, load ratio, experience, capacity, availability) and
 * selects the one with the highest composite score.
 */
@Slf4j
@Component
public class SmartCompositeRecommendationStrategy implements TechnicianAssignmentStrategy {

    private static final String STRATEGY_NAME = "smart-composite";

    private static final double SKILL_WEIGHT    = 0.30;
    private static final double LOAD_WEIGHT     = 0.25;
    private static final double EXP_WEIGHT      = 0.20;
    private static final double CAPACITY_WEIGHT = 0.15;
    private static final double AVAIL_WEIGHT    = 0.10;

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public Optional<Technician> assignTechnician(Incident incident, List<Technician> availableTechnicians) {
        if (availableTechnicians == null || availableTechnicians.isEmpty()) {
            return Optional.empty();
        }

        IncidentCategory category = incident.getCategory();

        Technician best = null;
        double bestScore = -1.0;

        for (Technician tech : availableTechnicians) {
            double skillMatch = tech.hasSkillFor(category) ? 1.0 : 0.0;
            double loadRatio = 1.0 - (tech.getCurrentWorkload() / (double) tech.getMaxWorkload());
            double expFactor = Math.min(tech.getYearsOfExperience() / 10.0, 1.0);
            double capacityFactor = (tech.getMaxWorkload() - tech.getCurrentWorkload()) / (double) tech.getMaxWorkload();
            double availFactor = tech.isAvailable() ? 1.0 : 0.0;

            double compositeScore = SKILL_WEIGHT * skillMatch
                                  + LOAD_WEIGHT * loadRatio
                                  + EXP_WEIGHT * expFactor
                                  + CAPACITY_WEIGHT * capacityFactor
                                  + AVAIL_WEIGHT * availFactor;

            log.debug("SmartComposite scoring for technician [{}]: "
                            + "skillMatch={} (×{}) + loadRatio={} (×{}) + expFactor={} (×{}) "
                            + "+ capacityFactor={} (×{}) + availFactor={} (×{}) = compositeScore={}",
                    tech.getId(),
                    skillMatch, SKILL_WEIGHT,
                    loadRatio, LOAD_WEIGHT,
                    expFactor, EXP_WEIGHT,
                    capacityFactor, CAPACITY_WEIGHT,
                    availFactor, AVAIL_WEIGHT,
                    compositeScore);

            if (compositeScore > bestScore) {
                bestScore = compositeScore;
                best = tech;
            }
        }

        if (best != null) {
            log.info("SmartComposite selected technician [{}] with composite score {} for incident [{}]",
                    best.getId(), bestScore, incident.getId());
        }

        return Optional.ofNullable(best);
    }
}
