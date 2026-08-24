package com.dms.hospitalService.hospital.kafka.producer;

import com.dms.hospitalService.hospital.kafka.event.InventoryShortageAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalKafkaProducer {

    // Using <String, Object> as we discussed earlier to support multiple event types and DLQs
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_INVENTORY_SHORTAGE = "inventory-shortage-topic";

    public void sendInventoryShortageAlert(InventoryShortageAlertEvent event) {
        log.warn("Publishing inventory shortage alert for Hospital: {} Item: {}",
                event.getHospitalId(), event.getItemType());

        kafkaTemplate.send(TOPIC_INVENTORY_SHORTAGE, event.getHospitalId().toString(), event);
    }
}