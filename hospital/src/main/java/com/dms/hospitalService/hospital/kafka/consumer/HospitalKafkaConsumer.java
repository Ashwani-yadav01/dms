package com.dms.hospitalService.hospital.kafka.consumer;

import com.dms.hospitalService.hospital.kafka.event.*;
import com.dms.hospitalService.hospital.kafka.event.SupplyDispatchedEvent;
import com.dms.hospitalService.hospital.service.InventoryService;
import com.dms.hospitalService.hospital.service.impl.HospitalSurgeServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalKafkaConsumer {

    private final HospitalSurgeServiceImpl hospitalSurgeService;
    private final InventoryService inventoryService;

    private static final double DEFAULT_HOSPITAL_RADIUS_KM = 15.0;
    private static final int DEFAULT_ESTIMATED_CASUALTIES = 15;

    // 1. Trigger when an incident is first reported
    @KafkaListener(topics = "incident-created-topic", groupId = "hospital-service-group")
    public void consumeIncidentCreated(IncidentCreatedEvent event) {
        log.info("🏥 [Hospital-Service] Incident detected: ID={}, Title='{}' at [{}, {}]",
                event.getIncidentId(), event.getTitle(), event.getLatitude(), event.getLongitude());

        if (event.getLatitude() == null || event.getLongitude() == null) {
            log.warn("Incident {} is missing coordinates. Skipping hospital surge alert.", event.getIncidentId());
            return;
        }

        hospitalSurgeService.alertNearbyHospitalsForIncident(
                event.getIncidentId(),
                event.getIncidentType() != null ? event.getIncidentType() : "DISASTER",
                event.getLatitude(),
                event.getLongitude(),
                DEFAULT_HOSPITAL_RADIUS_KM,
                DEFAULT_ESTIMATED_CASUALTIES
        );
    }

    // 2. Trigger when Logistics dispatches emergency supplies to this hospital
    @KafkaListener(topics = "${application.kafka.topic.supply-dispatched:supply-dispatched-topic}", groupId = "hospital-service-group")
    public void consumeSupplyDispatched(SupplyDispatchedEvent event) {
        log.info("🚚 [Hospital-Service] Incoming Supply Notice for Hospital ID: {}", event.getTargetHospitalId());
        log.info("Order: {}, Item: {}, Dispatched Qty: {} from Warehouse: {}",
                event.getDispatchOrderId(), event.getItemType(), event.getDispatchedQuantity(), event.getWarehouseName());
        log.info("Vehicle: {}, Status: {}, Estimated Arrival: {}",
                event.getVehicleNumber(), event.getStatus(), event.getEstimatedArrivalTime());

        // Automatically update the hospital inventory with the inbound restocking quantity
        if (event.getTargetHospitalId() != null && event.getItemType() != null && event.getDispatchedQuantity() != null) {
            inventoryService.restockItemFromDispatch(
                    event.getTargetHospitalId(),
                    event.getItemType(),
                    event.getDispatchedQuantity()
            );
        }
    }
}