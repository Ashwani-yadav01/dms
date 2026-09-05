package com.dms.hospitalService.hospital.service;

import com.dms.hospitalService.hospital.entity.Hospital;
import com.dms.hospitalService.hospital.kafka.event.InventoryShortageAlertEvent;
import com.dms.hospitalService.hospital.kafka.producer.HospitalKafkaProducer;
import com.dms.hospitalService.hospital.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HospitalInventoryAlertService {

    private final HospitalRepository hospitalRepository;
    private final HospitalKafkaProducer kafkaProducer;

    public void triggerShortageAlert(UUID hospitalId, String itemType, int requestedQty, String urgency) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found with ID: " + hospitalId));

        InventoryShortageAlertEvent alertEvent = InventoryShortageAlertEvent.builder()
                .hospitalId(hospital.getId())
                .hospitalName(hospital.getName())
                .hospitalLatitude(hospital.getLatitude())
                .hospitalLongitude(hospital.getLongitude())
                .itemType(itemType)
                .currentStock(0)
                .requestedQuantity(requestedQty)
                .urgencyLevel(urgency)
                .incomingCasualtyCount(0)
                .timestamp(Instant.now())
                .build();

        kafkaProducer.sendInventoryShortageAlert(alertEvent);

        log.info("🚨 Successfully triggered shortage alert for {}: {} units of {}",
                hospital.getName(), requestedQty, itemType);
    }
}