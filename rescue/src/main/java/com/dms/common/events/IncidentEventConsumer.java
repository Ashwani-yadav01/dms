package com.dms.common.events;

import com.dms.common.events.IncidentCreatedEvent;
import com.dms.rescueService.rescue.service.RescueAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IncidentEventConsumer {

    private final RescueAssignmentService assignmentService;

    @KafkaListener(
            topics = "${application.kafka.topic.incident-created:incident-created-topic}",
            groupId = "${spring.kafka.consumer.group-id:rescue-service-group}"
    )
    public void consumeIncidentCreated(IncidentCreatedEvent event) {
        log.info("Received IncidentCreatedEvent for ID: {}, Type: {}", event.getIncidentId(), event.getIncidentType());

        // Automatically assign a rescue team based on location & availability
        assignmentService.autoAssignRescueTeam(event);
    }
}