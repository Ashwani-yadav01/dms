package com.dms.notificationService.consumer;

import com.dms.notificationService.dto.event.EvacuationAlertEvent;
import com.dms.notificationService.dto.event.HospitalSurgeStandbyEvent;
import com.dms.notificationService.dto.event.SupplyDispatchedEvent;
import com.dms.notificationService.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertBroadcastKafkaConsumer {

    private final NotificationDispatchService dispatchService;

    @KafkaListener(topics = "evacuation-alert-topic", groupId = "notification-service-group")
    public void consumeEvacuationAlert(EvacuationAlertEvent event) {
        log.info("Consumed EvacuationAlertEvent for zone: {}", event.getHazardZoneName());
        dispatchService.dispatchMassEvacuationAlert(
                event.getTargetEmails(),
                event.getDisasterType(),
                event.getHazardZoneName(),
                event.getEvacuationInstructions()
        );
    }

    @KafkaListener(topics = "hospital-standby-topic", groupId = "notification-service-group")
    public void consumeHospitalStandby(HospitalSurgeStandbyEvent event) {
        log.info("Consumed HospitalSurgeStandbyEvent for hospital: {}", event.getHospitalName());
        dispatchService.dispatchHospitalStandbyAlert(
                event.getHospitalEmail(),
                event.getHospitalName(),
                event.getDisasterType(),
                event.getDistanceKm(),
                event.getEstimatedCasualties()
        );
    }

    @KafkaListener(topics = "supply-dispatched-topic", groupId = "notification-service-group")
    public void consumeSupplyDispatched(SupplyDispatchedEvent event) {
        log.info("Consumed SupplyDispatchedEvent for item: {}", event.getItemType());
        dispatchService.dispatchLogisticsArrivalAlert(
                event.getReceiverEmail(),
                event.getItemType(),
                event.getQuantity(),
                event.getVehicleNumber(),
                event.getWarehouseName(),
                event.getEstimatedArrival()
        );
    }
}