package com.dms.notificationService.consumer;

import com.dms.notificationService.dto.EvacuationAlertEvent;
import com.dms.notificationService.dto.HospitalSurgeStandbyEvent;
import com.dms.notificationService.service.SmsProvider;
import com.dms.notificationService.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DisasterAlertConsumer {

    private final SmsService smsService;
    private final SmsProvider smsProvider;
    @KafkaListener(topics = "evacuation-alert-topic", groupId = "notification-service-group")
    public void handleEvacuationAlert(EvacuationAlertEvent event) {
        log.warn("Received Evacuation Alert for {} targeting {} users", event.getDisasterType(), event.getTargetPhoneNumbers().size());

        String alertText = String.format(
                "EMERGENCY ALERT: %s detected near %s. Evacuate a 5km radius immediately. Follow local authorities.",
                event.getDisasterType(), event.getHazardZoneName()
        );

        // Fan-out SMS to all users in the blast radius
        for (String phoneNumber : event.getTargetPhoneNumbers()) {
            smsService.sendSms(phoneNumber, alertText);
        }
    }

    @KafkaListener(topics = "hospital-standby-topic", groupId = "notification-service-group")
    public void handleHospitalStandby(HospitalSurgeStandbyEvent event) {
        log.warn("Received Surge Standby for Hospital ID: {}", event.getHospitalId());

        String alertText = String.format(
                "SURGE WARNING: %s occurred %.1f km away. Prepare for incoming casualties and reserve ICU beds.",
                event.getDisasterType(), event.getDistanceKm()
        );

        smsService.sendSms(event.getHospitalPhoneNumber(), alertText);
    }

}