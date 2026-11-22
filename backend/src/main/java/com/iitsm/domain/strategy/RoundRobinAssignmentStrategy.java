package com.iitsm.domain.strategy;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.Technician;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round-robin technician assignment strategy.
 * Distributes incidents evenly across technicians using a thread-safe rotating counter.
 */
@Component
public class RoundRobinAssignmentStrategy implements TechnicianAssignmentStrategy {

    private static final String STRATEGY_NAME = "round-robin";

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public Optional<Technician> assignTechnician(Incident incident, List<Technician> availableTechnicians) {
        if (availableTechnicians == null || availableTechnicians.isEmpty()) {
            return Optional.empty();
        }

        int index = Math.abs(counter.getAndIncrement() % availableTechnicians.size());
        return Optional.of(availableTechnicians.get(index));
    }
}
