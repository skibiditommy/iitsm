package com.iitsm.service;

import com.iitsm.domain.enums.Priority;
import com.iitsm.domain.enums.SlaStatus;
import com.iitsm.domain.enums.Status;
import com.iitsm.domain.model.Incident;
import com.iitsm.dto.response.IncidentSummaryResponse;
import com.iitsm.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final IncidentRepository incidentRepository;

    /**
     * Multi-criteria search for incidents.
     */
    public List<IncidentSummaryResponse> search(String keyword, String status, String priority,
                                                  String category, String slaStatus,
                                                  Long technicianId, Long reporterId,
                                                  Long serviceId) {
        List<Incident> results = incidentRepository.findAll();

        if (keyword != null && !keyword.isBlank()) {
            String lower = keyword.toLowerCase();
            results = results.stream()
                    .filter(i -> (i.getTitle() != null && i.getTitle().toLowerCase().contains(lower))
                            || (i.getDescription() != null && i.getDescription().toLowerCase().contains(lower))
                            || (i.getIncidentId() != null && i.getIncidentId().toLowerCase().contains(lower)))
                    .collect(Collectors.toList());
        }
        if (status != null && !status.isBlank()) {
            Status s = Status.valueOf(status.toUpperCase());
            results = results.stream().filter(i -> i.getStatus() == s).collect(Collectors.toList());
        }
        if (priority != null && !priority.isBlank()) {
            Priority p = Priority.valueOf(priority.toUpperCase());
            results = results.stream().filter(i -> i.getCalculatedPriority() == p).collect(Collectors.toList());
        }
        if (category != null && !category.isBlank()) {
            results = results.stream()
                    .filter(i -> i.getCategory() != null && i.getCategory().getName().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }
        if (slaStatus != null && !slaStatus.isBlank()) {
            SlaStatus sl = SlaStatus.valueOf(slaStatus.toUpperCase());
            results = results.stream()
                    .filter(i -> i.getResponseSlaStatus() == sl || i.getResolutionSlaStatus() == sl)
                    .collect(Collectors.toList());
        }
        if (technicianId != null) {
            results = results.stream()
                    .filter(i -> i.getAssignedTechnician() != null
                            && i.getAssignedTechnician().getId().equals(technicianId))
                    .collect(Collectors.toList());
        }
        if (reporterId != null) {
            results = results.stream()
                    .filter(i -> i.getReporter() != null && i.getReporter().getId().equals(reporterId))
                    .collect(Collectors.toList());
        }
        if (serviceId != null) {
            results = results.stream()
                    .filter(i -> i.getAffectedService() != null
                            && i.getAffectedService().getId().equals(serviceId))
                    .collect(Collectors.toList());
        }

        return results.stream().map(i -> new IncidentSummaryResponse(
                i.getId(), i.getIncidentId(), i.getTitle(),
                i.getCalculatedPriority() != null ? i.getCalculatedPriority().name() : null,
                i.getStatus().name(),
                i.getCategory() != null ? i.getCategory().getName() : null,
                i.getAssignedTechnician() != null ? i.getAssignedTechnician().getFullName() : null,
                i.getResponseSlaStatus() != null ? i.getResponseSlaStatus().name() : null,
                i.getResolutionSlaStatus() != null ? i.getResolutionSlaStatus().name() : null,
                i.getCreatedAt()
        )).collect(Collectors.toList());
    }
}
