package com.dms.rescueService.rescue.kafka;

import com.dms.common.events.IncidentCreatedEvent;
import com.dms.rescueService.rescue.service.RescueAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerListener {

    private final RescueAssignmentService rescueAssignmentService;

    @KafkaListener(
            topics = "${application.kafka.topic.incident-created:incident-created-topic}",
            groupId = "${spring.kafka.consumer.group-id:rescue-service-group}",
            containerFactory = "incidentKafkaListenerContainerFactory"
    )
    public void consumeIncidentCreatedEvent(IncidentCreatedEvent event) {
        log.info("Received Kafka event for Incident ID: {}", event.getIncidentId());
        try {
            rescueAssignmentService.autoAssignRescueTeam(event);
        } catch (Exception e) {
            log.error("Failed to execute auto-dispatch for Incident ID: {}", event.getIncidentId(), e);
        }
    }
}