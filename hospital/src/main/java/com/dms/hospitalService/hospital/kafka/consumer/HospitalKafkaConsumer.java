package com.dms.hospitalService.hospital.kafka.consumer;

import com.dms.hospitalService.hospital.kafka.event.VictimsExtractedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalKafkaConsumer {

    @KafkaListener(topics = "victims-extracted-topic", groupId = "hospital-service-group")
    public void consumeVictimsExtracted(VictimsExtractedEvent event) {
        log.info("URGENT: Received incoming casualties from Mission: {} for Incident: {}",
                event.getMissionId(), event.getIncidentId());

        log.info("Alerting nearest hospitals to prepare for {} incoming victims.", event.getTotalVictims());
    }
}