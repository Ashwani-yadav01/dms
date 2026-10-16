package com.dms.hospitalService.hospital.kafka.consumer;

import com.dms.hospitalService.hospital.entity.HospitalDeliveryReceipt;
import com.dms.hospitalService.hospital.kafka.event.*;
import com.dms.hospitalService.hospital.kafka.event.SupplyDeliveredEvent;
import com.dms.hospitalService.hospital.kafka.event.SupplyDispatchedEvent;
import com.dms.hospitalService.hospital.repository.HospitalDeliveryReceiptRepository;
import com.dms.hospitalService.hospital.service.InventoryService;
import com.dms.hospitalService.hospital.service.impl.HospitalSurgeServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalKafkaConsumer {

    private final HospitalSurgeServiceImpl hospitalSurgeService;
    private final InventoryService inventoryService;
    private final HospitalDeliveryReceiptRepository deliveryReceiptRepository;

    private static final double DEFAULT_HOSPITAL_RADIUS_KM = 15.0;
    private static final int DEFAULT_ESTIMATED_CASUALTIES = 15;

    // 1. Trigger when an incident is first reported
    @KafkaListener(
            topics = "incident-created-topic",
            groupId = "hospital-service-group",
            containerFactory = "incidentCreatedKafkaListenerContainerFactory"
    )
    public void consumeIncidentCreated(IncidentCreatedEvent event) {
        log.info("🏥 [Hospital-Service] Incident detected: ID={}, Title='{}' at [{}, {}]",
                event.getIncidentId(),
                event.getTitle(),
                event.getLatitude(),
                event.getLongitude());

        if (event.getLatitude() == null || event.getLongitude() == null) {
            log.warn("Incident {} is missing coordinates. Skipping hospital surge alert.",
                    event.getIncidentId());
            return;
        }

        hospitalSurgeService.alertNearbyHospitalsForIncident(
                event.getIncidentId(),
                event.getIncidentType() != null
                        ? event.getIncidentType()
                        : "DISASTER",
                event.getLatitude(),
                event.getLongitude(),
                DEFAULT_HOSPITAL_RADIUS_KM,
                DEFAULT_ESTIMATED_CASUALTIES
        );
    }

    // 2. Trigger when Logistics dispatches emergency supplies to this hospital (In-Transit Notice)
    @KafkaListener(
            topics = "${application.kafka.topic.supply-dispatched:supply-dispatched-topic}",
            groupId = "hospital-service-group",
            containerFactory = "supplyDispatchedKafkaListenerContainerFactory"
    )
    public void consumeSupplyDispatched(SupplyDispatchedEvent event) {
        log.info("🚚 [Hospital-Service] In-Transit Supply Notice for Hospital ID: {}", event.getTargetHospitalId());
        log.info("Dispatch Order: {}, Item: {}, Dispatched Qty: {} from Warehouse: {}",
                event.getDispatchOrderId(), event.getItemType(), event.getDispatchedQuantity(), event.getWarehouseName());
        log.info("Vehicle: {}, Status: {}, Estimated Arrival: {}",
                event.getVehicleNumber(), event.getStatus(), event.getEstimatedArrivalTime());

        // In-transit telemetry notice: We do NOT eager-restock inventory here because supplies
        // are still en route on the road. Restocking strictly occurs upon actual physical delivery
        // confirmation via consumeSupplyDelivered.
    }

    // 3. Trigger when Logistics or Hospital confirms physical delivery of supplies
    @Transactional
    @KafkaListener(
            topics = "${application.kafka.topic.supply-delivered:supply-delivered-topic}",
            groupId = "hospital-service-group",
            containerFactory = "supplyDeliveredKafkaListenerContainerFactory"
    )
    public void consumeSupplyDelivered(SupplyDeliveredEvent event) {
        log.info("📦 [Hospital-Service] Supply Delivery Confirmed for Hospital ID: {}", event.getTargetHospitalId());
        log.info("Order: {}, Item: {}, Delivered Qty: {} from Warehouse: {}",
                event.getDispatchOrderId(), event.getItemType(), event.getDeliveredQuantity(), event.getWarehouseName());

        if (event.getDispatchOrderId() == null || event.getTargetHospitalId() == null) {
            log.warn("Missing dispatch order ID or hospital ID in SupplyDeliveredEvent. Skipping.");
            return;
        }

        // Idempotency / Exactly-once check
        if (deliveryReceiptRepository.existsByDispatchOrderId(event.getDispatchOrderId())) {
            log.warn("Dispatch order {} delivery receipt already processed. Skipping duplicate.", event.getDispatchOrderId());
            return;
        }

        int deliveredQty = event.getDeliveredQuantity() != null ? event.getDeliveredQuantity() : 0;
        if (deliveredQty > 0 && event.getItemType() != null) {
            inventoryService.restockItemFromDispatch(
                    event.getTargetHospitalId(),
                    event.getItemType(),
                    deliveredQty
            );
        }

        // Persist delivery receipt for tracking and audit trail
        HospitalDeliveryReceipt receipt = HospitalDeliveryReceipt.builder()
                .dispatchOrderId(event.getDispatchOrderId())
                .hospitalId(event.getTargetHospitalId())
                .sourceWarehouseId(event.getSourceWarehouseId())
                .warehouseName(event.getWarehouseName())
                .itemType(event.getItemType())
                .deliveredQuantity(deliveredQty)
                .vehicleNumber(event.getVehicleNumber())
                .receivedAt(event.getDeliveredAt() != null ? event.getDeliveredAt() : Instant.now())
                .build();

        deliveryReceiptRepository.save(receipt);
        log.info("✅ Saved HospitalDeliveryReceipt for order {}", event.getDispatchOrderId());
    }

    // 4. Trigger when Rescue completes extraction and reports casualties/victims
    @KafkaListener(
            topics = "${application.kafka.topic.victims-extracted:victims-extracted-topic}",
            groupId = "hospital-service-group",
            containerFactory = "victimsExtractedKafkaListenerContainerFactory"
    )
    public void consumeVictimsExtracted(VictimsExtractedEvent event) {
        log.info("🚨 [Hospital-Service] Victims extracted notification received: Mission ID: {}, Incident ID: {}, Total Victims: {}",
                event.getMissionId(), event.getIncidentId(), event.getTotalVictims());
    }
}