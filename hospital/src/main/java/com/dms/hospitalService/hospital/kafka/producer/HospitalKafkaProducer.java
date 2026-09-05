package com.dms.hospitalService.hospital.kafka.producer;

import com.dms.hospitalService.hospital.kafka.event.HospitalSurgeStandbyEvent;
import com.dms.hospitalService.hospital.kafka.event.InventoryShortageAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_INVENTORY_SHORTAGE = "inventory-shortage-topic";

    @Value("${application.kafka.topic.hospital-standby:hospital-standby-topic}")
    private String topicHospitalStandby;

    public void sendInventoryShortageAlert(InventoryShortageAlertEvent event) {
        log.warn("Publishing inventory shortage alert for Hospital: {} Item: {}",
                event.getHospitalId(), event.getItemType());

        kafkaTemplate.send(TOPIC_INVENTORY_SHORTAGE, event.getHospitalId().toString(), event);
    }

    public void sendHospitalSurgeStandbyAlert(HospitalSurgeStandbyEvent event) {
        log.warn("🏥 Publishing hospital surge standby alert for Hospital: {} (Incident: {})",
                event.getHospitalName(), event.getIncidentId());

        String key = event.getIncidentId() != null ? event.getIncidentId().toString() : event.getHospitalName();
        kafkaTemplate.send(topicHospitalStandby, key, event);
    }
}