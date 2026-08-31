package com.dms.notificationService.consumer;

import com.dms.notificationService.events.IncidentCreatedEvent;
import com.dms.notificationService.events.HospitalSurgeStandbyEvent;
import com.dms.notificationService.events.SupplyDispatchedEvent;
import com.dms.notificationService.client.UserServiceClient;
import com.dms.notificationService.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertBroadcastKafkaConsumer {

    private final NotificationDispatchService dispatchService;
    private final UserServiceClient userServiceClient;

    private static final double DEFAULT_HAZARD_RADIUS_KM = 5.0;

    @Value("${spring.mail.username}")
    private String defaultAdminEmail;

    // 1. INCIDENT CREATED: Fetch nearby users from User Service & broadcast email
    @KafkaListener(topics = "incident-created-topic", groupId = "notification-service-group")
    public void consumeIncidentCreated(IncidentCreatedEvent event) {
        log.info("📢 [Notification] Consumed IncidentCreatedEvent: ID={}, Title={}, Severity={}, Status={}",
                event.getIncidentId(), event.getTitle(), event.getSeverity(), event.getStatus());

        // Call User-Service via HTTP to find users within 5 km of the incident coordinates
        List<String> targetEmails = userServiceClient.fetchNearbyUserEmails(
                event.getLatitude(),
                event.getLongitude(),
                DEFAULT_HAZARD_RADIUS_KM
        );

        log.info("Found {} nearby citizen emails to notify for Incident ID: {}",
                targetEmails != null ? targetEmails.size() : 0, event.getIncidentId());

        String zoneLocation = String.format("%s (Coordinates: [%.4f, %.4f])",
                event.getTitle(), event.getLatitude(), event.getLongitude());

        String alertDetails = String.format(
                "Severity: %s\nStatus: %s\nHazard Type: %s\nRadius Affected: %.1f km\n\nDetails:\n%s",
                event.getSeverity(),
                event.getStatus(),
                event.getIncidentType(),
                DEFAULT_HAZARD_RADIUS_KM,
                event.getDescription() != null ? event.getDescription() : "Emergency alert in your area. Follow local safety procedures."
        );

        // Send to nearby users if found, otherwise forward copy to admin
        if (targetEmails != null && !targetEmails.isEmpty()) {
            dispatchService.dispatchMassEvacuationAlert(
                    targetEmails,
                    event.getIncidentType(),
                    zoneLocation,
                    alertDetails
            );
        } else {
            log.warn("No nearby users found within {} km. Forwarding alert to admin inbox: {}",
                    DEFAULT_HAZARD_RADIUS_KM, defaultAdminEmail);
            dispatchService.dispatchMassEvacuationAlert(
                    List.of(defaultAdminEmail),
                    event.getIncidentType(),
                    zoneLocation,
                    alertDetails
            );
        }
    }

    // 2. HOSPITAL SURGE ADVISORY
    @KafkaListener(topics = "hospital-standby-topic", groupId = "notification-service-group")
    public void consumeHospitalStandby(HospitalSurgeStandbyEvent event) {
        log.info("🏥 [Notification] Consumed HospitalSurgeStandbyEvent for: {}", event.getHospitalName());
        dispatchService.dispatchHospitalStandbyAlert(
                event.getHospitalEmail(),
                event.getHospitalName(),
                event.getDisasterType(),
                event.getDistanceKm(),
                event.getEstimatedCasualties()
        );
    }

    // 3. LOGISTICS DISPATCH
    @KafkaListener(topics = "supply-dispatched-topic", groupId = "notification-service-group")
    public void consumeSupplyDispatched(SupplyDispatchedEvent event) {
        log.info("🚚 [Notification] Consumed SupplyDispatchedEvent for item: {}", event.getItemType());
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