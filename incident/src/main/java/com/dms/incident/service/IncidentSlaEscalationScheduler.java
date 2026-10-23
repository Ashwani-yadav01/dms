package com.dms.incident.service;

import com.dms.common.events.IncidentOverdueEvent;
import com.dms.incident.entity.Incident;
import com.dms.incident.messaging.IncidentEventPublisher;
import com.dms.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class IncidentSlaEscalationScheduler {

    private final IncidentRepository incidentRepository;
    private final IncidentEventPublisher incidentEventPublisher;

    @Value("${application.incident.sla-threshold-hours:3}")
    private long slaThresholdHours;

    @Scheduled(cron = "${application.incident.sla-check-cron:0 */2 * * * *}")
    @Transactional
    public void scanAndEscalateOverdueIncidents() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(slaThresholdHours);
        List<Incident> overdueIncidents = incidentRepository.findOverdueIncompleteIncidents(threshold);

        if (overdueIncidents.isEmpty()) {
            return;
        }

        log.warn("⚠️ [SLA Monitor] Detected {} emergency incident(s) exceeding {}-hour resolution SLA window!",
                overdueIncidents.size(), slaThresholdHours);

        for (Incident incident : overdueIncidents) {
            long elapsedMinutes = Duration.between(incident.getCreatedAt(), LocalDateTime.now()).toMinutes();

            Set<String> deptCategories = IncidentDepartmentPolicy.getAuthorizedDepartmentCategories(incident.getIncidentType());
            String primaryDeptCategory = deptCategories.stream()
                    .filter(c -> !"DEPARTMENT_MANAGEMENT".equals(c))
                    .findFirst()
                    .orElse("DEPARTMENT_MANAGEMENT");

            IncidentOverdueEvent event = IncidentOverdueEvent.builder()
                    .incidentId(incident.getId())
                    .title(incident.getTitle())
                    .description(incident.getDescription())
                    .incidentType(incident.getIncidentType() != null ? incident.getIncidentType().name() : null)
                    .severity(incident.getSeverity() != null ? incident.getSeverity().name() : "MEDIUM")
                    .status(incident.getStatus() != null ? incident.getStatus().name() : "REPORTED")
                    .latitude(incident.getLatitude())
                    .longitude(incident.getLongitude())
                    .targetDepartmentCategory(primaryDeptCategory)
                    .createdAt(incident.getCreatedAt())
                    .elapsedMinutes(elapsedMinutes)
                    .build();

            incidentEventPublisher.publishIncidentOverdue(event);

            incident.setOverdueNotified(true);
            incidentRepository.save(incident);

            log.warn("🚨 [SLA Escalation Alert Sent] Incident #{} [{}] is incomplete after {} hours ({} min). Department: {}",
                    incident.getId(), incident.getTitle(), elapsedMinutes / 60, elapsedMinutes, primaryDeptCategory);
        }
    }
}
