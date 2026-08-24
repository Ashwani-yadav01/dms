package com.dms.rescueService.rescue.kafka;

import com.dms.common.events.VictimsExtractedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RescueKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_VICTIMS_EXTRACTED = "victims-extracted-topic";

    public void sendVictimsExtractedEvent(VictimsExtractedEvent event) {
        log.info("Emitting VictimsExtractedEvent for Mission: {} to alert Hospital Service", event.getMissionId());

        // We use the incidentId as the Kafka key to ensure messages for the same incident stay in order
        kafkaTemplate.send(TOPIC_VICTIMS_EXTRACTED, event.getIncidentId().toString(), event);
    }
}