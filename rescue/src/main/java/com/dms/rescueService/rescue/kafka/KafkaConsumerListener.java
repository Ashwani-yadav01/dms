package com.dms.rescueService.rescue.kafka;

import com.dms.common.events.IncidentCreatedEvent;
import com.dms.rescueService.rescue.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerListener {

    private final DepartmentService departmentService;

    @KafkaListener(
            topics = "${spring.kafka.topic.name:incident-created-topic}",
            groupId = "${spring.kafka.consumer.group-id:rescue-service-group}"
    )
    public void consumeIncidentCreatedEvent(IncidentCreatedEvent event) {
        log.info("Received Kafka event for Incident ID: {}", event.getIncidentId());
        departmentService.processAutoDispatchForIncident(event);
    }
}