package com.iitsm.controller;

import com.iitsm.config.StrategyConfigService;
import com.iitsm.dto.request.StrategyUpdateRequest;
import com.iitsm.dto.response.StrategyConfigResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/strategies")
@RequiredArgsConstructor
public class StrategyController {

    private final StrategyConfigService strategyConfigService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StrategyConfigResponse> getConfig() {
        return ResponseEntity.ok(new StrategyConfigResponse(
                strategyConfigService.getActivePriorityStrategy().getStrategyName(),
                strategyConfigService.getActiveAssignmentStrategy().getStrategyName(),
                strategyConfigService.getAvailablePriorityStrategies(),
                strategyConfigService.getAvailableAssignmentStrategies()
        ));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StrategyConfigResponse> update(@Valid @RequestBody StrategyUpdateRequest request) {
        if ("priority".equalsIgnoreCase(request.strategyType())) {
            strategyConfigService.switchPriorityStrategy(request.strategyName());
        } else if ("assignment".equalsIgnoreCase(request.strategyType())) {
            strategyConfigService.switchAssignmentStrategy(request.strategyName());
        } else {
            throw new IllegalArgumentException("strategyType must be 'priority' or 'assignment'");
        }
        return getConfig();
    }
}
