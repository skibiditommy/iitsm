package com.iitsm.config;

import com.iitsm.domain.strategy.PriorityCalculationStrategy;
import com.iitsm.domain.strategy.TechnicianAssignmentStrategy;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic registry of priority and assignment strategies with runtime switching.
 * Strategies can be swapped at runtime without restarting the application.
 */
@Slf4j
@Service
public class StrategyConfigService {

    private final Map<String, PriorityCalculationStrategy> priorityStrategies = new HashMap<>();
    private final Map<String, TechnicianAssignmentStrategy> assignmentStrategies = new HashMap<>();

    @Getter
    private volatile PriorityCalculationStrategy activePriorityStrategy;
    @Getter
    private volatile TechnicianAssignmentStrategy activeAssignmentStrategy;

    public StrategyConfigService(List<PriorityCalculationStrategy> priorityList,
                                  List<TechnicianAssignmentStrategy> assignmentList) {
        for (PriorityCalculationStrategy s : priorityList) {
            priorityStrategies.put(s.getStrategyName(), s);
        }
        for (TechnicianAssignmentStrategy s : assignmentList) {
            assignmentStrategies.put(s.getStrategyName(), s);
        }
    }

    @PostConstruct
    public void init() {
        // Default strategies
        activePriorityStrategy = priorityStrategies.getOrDefault("weighted-scoring",
                priorityStrategies.values().iterator().next());
        activeAssignmentStrategy = assignmentStrategies.getOrDefault("smart-composite",
                assignmentStrategies.values().iterator().next());
        log.info("Default strategies - Priority: {}, Assignment: {}",
                activePriorityStrategy.getStrategyName(), activeAssignmentStrategy.getStrategyName());
    }

    public void switchPriorityStrategy(String name) {
        PriorityCalculationStrategy strategy = priorityStrategies.get(name);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown priority strategy: " + name
                    + ". Available: " + priorityStrategies.keySet());
        }
        activePriorityStrategy = strategy;
        log.info("Switched priority strategy to: {}", name);
    }

    public void switchAssignmentStrategy(String name) {
        TechnicianAssignmentStrategy strategy = assignmentStrategies.get(name);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown assignment strategy: " + name
                    + ". Available: " + assignmentStrategies.keySet());
        }
        activeAssignmentStrategy = strategy;
        log.info("Switched assignment strategy to: {}", name);
    }

    public List<String> getAvailablePriorityStrategies() {
        return List.copyOf(priorityStrategies.keySet());
    }

    public List<String> getAvailableAssignmentStrategies() {
        return List.copyOf(assignmentStrategies.keySet());
    }
}
