package com.dms.rescueService.rescue.kafka;

import com.dms.common.events.IncidentCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeadLetterQueueConsumer {

    @KafkaListener(
            topics = "${application.kafka.topic.incident-created:incident-created-topic}.DLT",
            groupId = "rescue-service-dlq-group"
    )
    public void consumeDlq(IncidentCreatedEvent failedEvent) {
        log.error("ALERT: Received message in DLQ for Incident ID: {}. Triggering manual alert.",
                failedEvent.getIncidentId());
        // Save to DB or trigger alert notification here
    }
}