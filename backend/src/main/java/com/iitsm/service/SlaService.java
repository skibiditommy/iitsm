package com.iitsm.service;

import com.iitsm.domain.enums.SlaStatus;
import com.iitsm.domain.enums.Status;
import com.iitsm.domain.model.Incident;
import com.iitsm.dto.response.SlaRiskAssessmentResponse;
import com.iitsm.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaService {

    private final IncidentRepository incidentRepository;
    private final DemoTimeService demoTimeService;

    /**
     * Checks all active incidents for SLA compliance and updates their status.
     */
    public void checkAndUpdateSlaStatus() {
        List<Incident> activeIncidents = incidentRepository.findActiveIncidents();
        LocalDateTime now = demoTimeService.now();

        for (Incident incident : activeIncidents) {
            if (incident.getStatus() == Status.PENDING_USER) {
                if (incident.getResolutionSlaStatus() != SlaStatus.PAUSED) {
                    incident.updateSlaStatus(incident.getResponseSlaStatus(), SlaStatus.PAUSED);
                    incidentRepository.save(incident);
                }
                continue; // SLA clock paused
            }
            boolean changed = false;

            // Check response SLA
            if (incident.getResponseDeadline() != null && incident.getRespondedAt() == null) {
                long minutesRemaining = Duration.between(now, incident.getResponseDeadline()).toMinutes();
                SlaStatus newResponseStatus;
                if (minutesRemaining < 0) {
                    newResponseStatus = SlaStatus.BREACHED;
                } else if (minutesRemaining <= 15) {
                    newResponseStatus = SlaStatus.AT_RISK;
                } else {
                    newResponseStatus = SlaStatus.ON_TRACK;
                }
                if (newResponseStatus != incident.getResponseSlaStatus()) {
                    incident.updateSlaStatus(newResponseStatus, incident.getResolutionSlaStatus());
                    changed = true;
                }
            }

            // Check resolution SLA
            if (incident.getResolutionDeadline() != null) {
                long minutesRemaining = Duration.between(now, incident.getResolutionDeadline()).toMinutes();
                SlaStatus newResolutionStatus;
                if (minutesRemaining < 0) {
                    newResolutionStatus = SlaStatus.BREACHED;
                } else if (minutesRemaining <= 30) {
                    newResolutionStatus = SlaStatus.AT_RISK;
                } else {
                    newResolutionStatus = SlaStatus.ON_TRACK;
                }
                if (newResolutionStatus != incident.getResolutionSlaStatus()) {
                    incident.updateSlaStatus(incident.getResponseSlaStatus(), newResolutionStatus);
                    changed = true;
                }
            }

            if (changed) {
                incidentRepository.save(incident);
                log.info("SLA status updated for {}: response={}, resolution={}",
                        incident.getIncidentId(), incident.getResponseSlaStatus(),
                        incident.getResolutionSlaStatus());
            }
        }
    }

    public List<SlaRiskAssessmentResponse> getActiveRiskAssessments() {
        List<Incident> active = incidentRepository.findActiveIncidents();
        LocalDateTime now = demoTimeService.now();

        return active.stream().map(i -> {
            long responseRemaining = i.getResponseDeadline() != null
                    ? Duration.between(now, i.getResponseDeadline()).toMinutes() : Long.MAX_VALUE;
            long resolutionRemaining = i.getResolutionDeadline() != null
                    ? Duration.between(now, i.getResolutionDeadline()).toMinutes() : Long.MAX_VALUE;

            String riskLevel;
            if (responseRemaining < 0 || resolutionRemaining < 0) riskLevel = "HIGH";
            else if (responseRemaining <= 15 || resolutionRemaining <= 30) riskLevel = "MEDIUM";
            else riskLevel = "LOW";

            return new SlaRiskAssessmentResponse(
                    i.getIncidentId(),
                    i.getResponseSlaStatus() != null ? i.getResponseSlaStatus().name() : "ON_TRACK",
                    i.getResolutionSlaStatus() != null ? i.getResolutionSlaStatus().name() : "ON_TRACK",
                    responseRemaining, resolutionRemaining, riskLevel
            );
        }).collect(Collectors.toList());
    }
}
