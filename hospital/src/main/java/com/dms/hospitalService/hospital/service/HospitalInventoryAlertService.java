package com.dms.hospitalService.hospital.service;

import com.dms.hospitalService.hospital.entity.Hospital;
import com.dms.hospitalService.hospital.kafka.event.InventoryShortageAlertEvent;
import com.dms.hospitalService.hospital.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HospitalInventoryAlertService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final HospitalRepository hospitalRepository;

    private static final String SHORTAGE_TOPIC = "inventory-shortage-topic";

    public void triggerShortageAlert(UUID hospitalId, String itemType, int requestedQty, String urgency) {
        // Fetch hospital details to include GPS coordinates for logistics routing
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("Hospital not found"));

        InventoryShortageAlertEvent alertEvent = InventoryShortageAlertEvent.builder()
                .hospitalId(hospital.getId())
                .hospitalName(hospital.getName())
                .hospitalLatitude(hospital.getLatitude())
                .hospitalLongitude(hospital.getLongitude())
                .itemType(itemType)
                .currentStock(0) // You would normally pull this from the DB
                .requestedQuantity(requestedQty)
                .urgencyLevel(urgency)
                .incomingCasualtyCount(0)
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(SHORTAGE_TOPIC, hospital.getId().toString(), alertEvent);
        log.info("🚨 Published Shortage Alert for {}: {} units of {}",
                hospital.getName(), requestedQty, itemType);
    }
}