package com.dms.incident.kafka;

import com.dms.common.events.RescueMissionStatusUpdatedEvent;
import com.dms.incident.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RescueStatusConsumerService {

    private final IncidentService incidentService;

    @KafkaListener(
            topics = "rescue-mission-status-topic",
            groupId = "incident-service-group"
    )
    public void consumeStatusUpdate(@Payload RescueMissionStatusUpdatedEvent event) {
        log.info("Received mission update for Incident ID: {}, Status: {}",
                event.getIncidentId(), event.getStatus());

        String statusName = event.getStatus() != null ? event.getStatus(): null;

        incidentService.updateStatusFromRescueEvent(
                event.getIncidentId(),
                statusName,
                event.getNotes()
        );

        // 2. Push real-time event to End User
//        incidentService.notifyEndUser(event.getIncidentId(), event.getStatus(), event.getNotes());
    }
}