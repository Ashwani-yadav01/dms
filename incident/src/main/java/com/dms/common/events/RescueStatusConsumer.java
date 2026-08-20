package com.dms.common.events;

import com.dms.common.events.RescueMissionStatusUpdatedEvent;
import com.dms.incident.service.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RescueStatusConsumer {

    private final IncidentService incidentService;

    @KafkaListener(
            topics = "${app.kafka.topics.rescue-mission-status:rescue-mission-status-topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeStatusUpdate(RescueMissionStatusUpdatedEvent event) {
        log.info("Received status update event for Incident ID: {}, Status: {}",
                event.getIncidentId(), event.getStatus());

        // Updates the Incident Service DB view for end-users/dashboard
        incidentService.updateIncidentStatusFromRescue(
                event.getIncidentId(),
                event.getStatus(),
                event.getNotes()
        );
    }
}