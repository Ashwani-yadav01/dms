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

        // Let the exception bubble up!
        // If this throws, Spring Kafka will catch it, trigger the 3 retries, and then send to DLQ.
        rescueAssignmentService.autoAssignRescueTeam(event);
    }
}