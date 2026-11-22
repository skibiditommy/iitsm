package com.iitsm.dto.response;

public record StrategyConfigResponse(String activePriorityStrategy, String activeAssignmentStrategy,
                                       java.util.List<String> availablePriorityStrategies,
                                       java.util.List<String> availableAssignmentStrategies) {}
