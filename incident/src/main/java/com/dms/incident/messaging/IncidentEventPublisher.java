package com.dms.incident.messaging;

import com.dms.common.events.IncidentCreatedEvent;
import com.dms.common.events.IncidentOverdueEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IncidentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${application.kafka.topic.incident-created}")
    private String incidentCreatedTopic;

    @Value("${application.kafka.topic.incident-overdue:incident-overdue-topic}")
    private String incidentOverdueTopic;

    public void publishIncidentCreated(IncidentCreatedEvent event) {
        log.info("Publishing IncidentCreatedEvent for Incident ID: {} to topic: {}",
                event.getIncidentId(), incidentCreatedTopic);

        kafkaTemplate.send(incidentCreatedTopic, event.getIncidentId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published IncidentCreatedEvent [ID: {}] at offset: {}",
                                event.getIncidentId(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish IncidentCreatedEvent [ID: {}] due to: {}",
                                event.getIncidentId(), ex.getMessage(), ex);
                    }
                });
    }

    public void publishIncidentOverdue(IncidentOverdueEvent event) {
        log.warn("🚨 [SLA Breach] Publishing IncidentOverdueEvent for Incident ID: {} to topic: {}",
                event.getIncidentId(), incidentOverdueTopic);

        kafkaTemplate.send(incidentOverdueTopic, event.getIncidentId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published IncidentOverdueEvent [ID: {}] at offset: {}",
                                event.getIncidentId(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish IncidentOverdueEvent [ID: {}] due to: {}",
                                event.getIncidentId(), ex.getMessage(), ex);
                    }
                });
    }
}
