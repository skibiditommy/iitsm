package com.iitsm.service;

import com.iitsm.domain.enums.Status;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.ITService;
import com.iitsm.domain.model.Technician;
import com.iitsm.dto.response.*;
import com.iitsm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final IncidentRepository incidentRepository;
    private final TechnicianRepository technicianRepository;
    private final ITServiceRepository serviceRepository;
    private final IncidentCategoryRepository categoryRepository;

    public AnalyticsOverviewResponse getOverview() {
        List<Incident> all = incidentRepository.findAll();
        long total = all.size();
        long active = all.stream().filter(Incident::isActive).count();
        long resolved = incidentRepository.countByStatus(Status.RESOLVED);
        long closed = incidentRepository.countByStatus(Status.CLOSED);
        long slaBreached = all.stream().filter(Incident::isSlaBreached).count();

        double avgResolution = all.stream()
                .filter(i -> i.getResolvedAt() != null && i.getCreatedAt() != null)
                .mapToLong(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt()).toMinutes())
                .average().orElse(0);

        double avgSatisfaction = all.stream()
                .filter(i -> i.getSatisfactionRating() != null)
                .mapToInt(Incident::getSatisfactionRating)
                .average().orElse(0);

        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(i -> i.getStatus().name(), Collectors.counting()));
        Map<String, Long> byPriority = all.stream()
                .filter(i -> i.getCalculatedPriority() != null)
                .collect(Collectors.groupingBy(i -> i.getCalculatedPriority().name(), Collectors.counting()));
        Map<String, Long> byCategory = all.stream()
                .filter(i -> i.getCategory() != null)
                .collect(Collectors.groupingBy(i -> i.getCategory().getName(), Collectors.counting()));

        return new AnalyticsOverviewResponse(total, active, resolved, closed,
                slaBreached, avgResolution, avgSatisfaction, byStatus, byPriority, byCategory);
    }

    public List<TechnicianWorkloadResponse> getTechnicianWorkloads() {
        return technicianRepository.findAll().stream().map(t -> {
            double utilization = t.getMaxWorkload() > 0
                    ? (t.getCurrentWorkload() * 100.0 / t.getMaxWorkload()) : 0;
            return new TechnicianWorkloadResponse(t.getId(), t.getFullName(), t.getEmail(),
                    t.getCurrentWorkload(), t.getMaxWorkload(), utilization, t.isAvailable());
        }).collect(Collectors.toList());
    }

    public List<ServiceHealthResponse> getServiceHealth() {
        return serviceRepository.findAll().stream().map(s -> {
            List<Incident> serviceIncidents = incidentRepository.findByAffectedServiceId(s.getId());
            long activeCount = serviceIncidents.stream().filter(Incident::isActive).count();
            long slaBreached = serviceIncidents.stream().filter(Incident::isSlaBreached).count();
            int blastRadius = s.getBlastRadius().size();

            String health;
            if (slaBreached > 0) health = "CRITICAL";
            else if (activeCount > 3) health = "DEGRADED";
            else if (activeCount > 0) health = "WARNING";
            else health = "HEALTHY";

            return new ServiceHealthResponse(s.getId(), s.getName(), s.getBusinessCriticality(),
                    activeCount, slaBreached, blastRadius, health);
        }).collect(Collectors.toList());
    }

    public List<ServiceImpactResponse> getServiceImpacts() {
        Map<Long, List<ServiceImpactResponse.Ticket>> tickets = incidentRepository.findActiveIncidents().stream()
                .filter(i -> i.getAffectedService() != null)
                .sorted(Comparator.comparing(Incident::getId))
                .collect(Collectors.groupingBy(i -> i.getAffectedService().getId(),
                        Collectors.mapping(i -> new ServiceImpactResponse.Ticket(i.getId(), i.getIncidentId(),
                                i.getTitle()), Collectors.toList())));

        return serviceRepository.findAll().stream().map(service -> {
            var dependents = service.getImpactPaths().stream().map(path -> {
                Long id = path.getLast().getId();
                return new ServiceImpactResponse.Dependent(id, path.stream().map(ITService::getName).toList(),
                        tickets.getOrDefault(id, List.of()));
            }).toList();
            long affected = dependents.stream().filter(d -> !d.activeTickets().isEmpty()).count();
            return new ServiceImpactResponse(service.getId(), service.getName(),
                    tickets.getOrDefault(service.getId(), List.of()), dependents, affected);
        }).sorted(Comparator.comparingLong(ServiceImpactResponse::affectedDependentCount).reversed()
                .thenComparing(ServiceImpactResponse::name)).toList();
    }

    public List<RootCauseSuggestionResponse> getRootCauseSuggestions() {
        List<ServiceImpactResponse> impacts = getServiceImpacts();
        return impacts.stream().map(imp -> {
            ITService service = serviceRepository.findById(imp.id()).orElse(null);
            int crit = service != null ? service.getBusinessCriticality() : 3;
            int blast = service != null ? service.getBlastRadius().size() : 0;

            List<String> affectedNames = imp.dependents().stream()
                    .filter(d -> !d.activeTickets().isEmpty())
                    .map(d -> d.path().isEmpty() ? "Unknown" : d.path().get(d.path().size() - 1))
                    .distinct().toList();

            List<String> activeIds = new ArrayList<>();
            imp.activeTickets().forEach(t -> activeIds.add(t.incidentId()));
            imp.dependents().forEach(d -> d.activeTickets().forEach(t -> activeIds.add(t.incidentId())));

            double confidence = Math.min(99.0, imp.affectedDependentCount() * 22.0 + crit * 4.0);

            String recommendation;
            if (imp.affectedDependentCount() >= 3) {
                recommendation = "CRITICAL: " + imp.affectedDependentCount()
                        + " downstream services are experiencing active disruptions (" + String.join(", ", affectedNames)
                        + "). " + imp.name() + " is strongly indicated as the common root cause.";
            } else if (imp.affectedDependentCount() >= 1) {
                recommendation = "MODERATE: Disruptions in " + String.join(", ", affectedNames)
                        + " may stem from upstream dependency on " + imp.name() + ".";
            } else {
                recommendation = "HEALTHY: No correlated downstream failures detected.";
            }

            return new RootCauseSuggestionResponse(
                    imp.id(), imp.name(), crit, blast,
                    (int) imp.affectedDependentCount(),
                    affectedNames, activeIds, confidence, recommendation
            );
        }).toList();
    }

    public List<CorrelationClusterResponse> getCorrelationClusters() {
        List<Incident> active = incidentRepository.findActiveIncidents();
        Map<String, List<Incident>> byService = active.stream()
                .filter(i -> i.getAffectedService() != null)
                .collect(Collectors.groupingBy(i -> i.getAffectedService().getName()));

        List<CorrelationClusterResponse> clusters = new ArrayList<>();
        for (Map.Entry<String, List<Incident>> entry : byService.entrySet()) {
            if (entry.getValue().size() >= 2) {
                List<String> ids = entry.getValue().stream()
                        .map(Incident::getIncidentId).collect(Collectors.toList());
                clusters.add(new CorrelationClusterResponse(
                        "Service: " + entry.getKey(),
                        "Multiple active incidents affecting " + entry.getKey(),
                        ids
                ));
            }
        }
        return clusters;
    }
}
