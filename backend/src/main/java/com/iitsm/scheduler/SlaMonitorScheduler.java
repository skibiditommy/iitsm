package com.iitsm.scheduler;

import com.iitsm.domain.enums.SlaStatus;
import com.iitsm.domain.enums.Status;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.observer.*;
import com.iitsm.repository.IncidentRepository;
import com.iitsm.service.DemoTimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that checks active incidents for SLA compliance.
 * Calculates elapsed %, emits SlaApproaching and SlaViolated events.
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SlaMonitorScheduler {

    private final IncidentRepository incidentRepository;
    private final IncidentEventPublisher eventPublisher;
    private final DemoTimeService demoTimeService;

    @Scheduled(fixedRate = 60000) // every 60 seconds
    @Transactional
    public void checkSlaCompliance() {
        List<Incident> activeIncidents = incidentRepository.findActiveIncidents();
        LocalDateTime now = demoTimeService.now();

        for (Incident incident : activeIncidents) {
            if (incident.getStatus() == Status.PENDING_USER) {
                // SLA Clock is paused while in PENDING_USER waiting on employee response
                continue;
            }
            checkResponseSla(incident, now);
            checkResolutionSla(incident, now);
        }
    }

    /**
     * Manual trigger for demo purposes.
     */
    public void triggerCheck() {
        log.info("Manual SLA check triggered");
        checkSlaCompliance();
    }

    private void checkResponseSla(Incident incident, LocalDateTime now) {
        if (incident.getResponseDeadline() == null || incident.getRespondedAt() != null) return;

        long minutesRemaining = Duration.between(now, incident.getResponseDeadline()).toMinutes();

        if (minutesRemaining < 0 && incident.getResponseSlaStatus() != SlaStatus.BREACHED) {
            incident.updateSlaStatus(SlaStatus.BREACHED, incident.getResolutionSlaStatus());
            incidentRepository.save(incident);
            eventPublisher.publish(new SlaViolatedEvent(
                    incident.getId(), incident.getIncidentId(),
                    "RESPONSE", Math.abs(minutesRemaining), LocalDateTime.now()
            ));
            log.warn("RESPONSE SLA BREACHED for {} by {} minutes", incident.getIncidentId(), Math.abs(minutesRemaining));
        } else if (minutesRemaining >= 0 && minutesRemaining <= 15
                && incident.getResponseSlaStatus() == SlaStatus.ON_TRACK) {
            incident.updateSlaStatus(SlaStatus.AT_RISK, incident.getResolutionSlaStatus());
            incidentRepository.save(incident);
            eventPublisher.publish(new SlaApproachingEvent(
                    incident.getId(), incident.getIncidentId(),
                    "RESPONSE", minutesRemaining, LocalDateTime.now()
            ));
            log.warn("RESPONSE SLA approaching for {}: {} minutes remaining", incident.getIncidentId(), minutesRemaining);
        }
    }

    private void checkResolutionSla(Incident incident, LocalDateTime now) {
        if (incident.getResolutionDeadline() == null) return;

        long minutesRemaining = Duration.between(now, incident.getResolutionDeadline()).toMinutes();

        if (minutesRemaining < 0 && incident.getResolutionSlaStatus() != SlaStatus.BREACHED) {
            incident.updateSlaStatus(incident.getResponseSlaStatus(), SlaStatus.BREACHED);
            incidentRepository.save(incident);
            eventPublisher.publish(new SlaViolatedEvent(
                    incident.getId(), incident.getIncidentId(),
                    "RESOLUTION", Math.abs(minutesRemaining), LocalDateTime.now()
            ));
            log.warn("RESOLUTION SLA BREACHED for {} by {} minutes", incident.getIncidentId(), Math.abs(minutesRemaining));
        } else if (minutesRemaining >= 0 && minutesRemaining <= 30
                && incident.getResolutionSlaStatus() == SlaStatus.ON_TRACK) {
            incident.updateSlaStatus(incident.getResponseSlaStatus(), SlaStatus.AT_RISK);
            incidentRepository.save(incident);
            eventPublisher.publish(new SlaApproachingEvent(
                    incident.getId(), incident.getIncidentId(),
                    "RESOLUTION", minutesRemaining, LocalDateTime.now()
            ));
            log.warn("RESOLUTION SLA approaching for {}: {} minutes remaining", incident.getIncidentId(), minutesRemaining);
        }
    }
}
