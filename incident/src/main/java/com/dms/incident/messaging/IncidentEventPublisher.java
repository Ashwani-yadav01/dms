package com.dms.incident.messaging;

import com.dms.common.events.IncidentCreatedEvent;
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
    private String topicName;

    public void publishIncidentCreated(IncidentCreatedEvent event) {
        log.info("Publishing IncidentCreatedEvent for Incident ID: {} to topic: {}",
                event.getIncidentId(), topicName);

        kafkaTemplate.send(topicName, event.getIncidentId().toString(), event)
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
}
