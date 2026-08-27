package com.dms.hospitalService.hospital.kafka.consumer;

import com.dms.hospitalService.hospital.kafka.event.SupplyDispatchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogisticsResponseConsumer {

    @KafkaListener(
            topics = "supply-dispatched-topic",
            groupId = "hospital-service-group"
    )
    public void consumeDispatchUpdate(SupplyDispatchedEvent event) {
        log.info("✅ GREAT NEWS! Supplies are En Route to Hospital ID: {}", event.getTargetHospitalId());
        log.info("📦 Item: {} | Qty: {} | Coming From: {}",
                event.getItemType(), event.getDispatchedQuantity(), event.getWarehouseName());
        log.info("🚚 Vehicle: {} | ETA: {}",
                event.getVehicleNumber(), event.getEstimatedArrivalTime());

        // Next Step: You could save this into an 'IncomingShipments' DB table
        // or push it to a WebSocket for the hospital's frontend dashboard.
    }
}
