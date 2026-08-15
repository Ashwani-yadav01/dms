package com.dms.rescueService.rescue.messaging;

import com.dms.common.events.IncidentCreatedEvent;
import com.dms.rescueService.rescue.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentCreatedEventListener {

    private final DepartmentService departmentService;

    @KafkaListener(
            topics = "${application.kafka.topic.incident-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleIncidentCreated(IncidentCreatedEvent event) {
        log.info("Kafka Listener consumed IncidentCreatedEvent for ID: {}", event.getIncidentId());
        departmentService.processAutoDispatchForIncident(event);
    }
}