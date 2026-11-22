package com.iitsm.controller;

import com.iitsm.dto.response.*;
import com.iitsm.service.AnalyticsService;
import com.iitsm.service.SlaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final SlaService slaService;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnalyticsOverviewResponse> getOverview() {
        return ResponseEntity.ok(analyticsService.getOverview());
    }

    @GetMapping("/technician-workloads")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TechnicianWorkloadResponse>> getWorkloads() {
        return ResponseEntity.ok(analyticsService.getTechnicianWorkloads());
    }

    @GetMapping("/service-health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ServiceHealthResponse>> getServiceHealth() {
        return ResponseEntity.ok(analyticsService.getServiceHealth());
    }

    @GetMapping("/service-impacts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ServiceImpactResponse>> getServiceImpacts() {
        return ResponseEntity.ok(analyticsService.getServiceImpacts());
    }

    @GetMapping("/root-cause-suggestions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RootCauseSuggestionResponse>> getRootCauseSuggestions() {
        return ResponseEntity.ok(analyticsService.getRootCauseSuggestions());
    }

    @GetMapping("/sla-risk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SlaRiskAssessmentResponse>> getSlaRisk() {
        return ResponseEntity.ok(slaService.getActiveRiskAssessments());
    }

    @GetMapping("/correlation-clusters")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CorrelationClusterResponse>> getCorrelations() {
        return ResponseEntity.ok(analyticsService.getCorrelationClusters());
    }
}
