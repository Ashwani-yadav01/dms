package com.dms.notificationService.service;

import com.dms.notificationService.entity.NotificationLog;
import com.dms.notificationService.entity.NotificationStatus;
import com.dms.notificationService.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchService {

    private final JavaMailSender mailSender;
    private final NotificationLogRepository logRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Transactional
    public void dispatchMassEvacuationAlert(List<String> targetEmails, String disasterType, String zoneName, String instructions) {
        String subject = "🚨 CRITICAL EVACUATION ALERT: " + zoneName;
        String alertMessage = String.format(
                "DISASTER EMERGENCY WARNING\n\n" +
                        "Hazard: %s detected near %s.\n" +
                        "Instruction: Evacuate a 5km perimeter immediately.\n" +
                        "Guidance: %s\n\n" +
                        "-- Disaster Management System Automated Dispatch",
                disasterType, zoneName, (instructions != null ? instructions : "Follow emergency crew directions.")
        );

        if (targetEmails != null) {
            for (String email : targetEmails) {
                sendAndLogEmail(email, subject, alertMessage, "EVACUATION");
            }
        }
    }

    @Transactional
    public void dispatchHospitalStandbyAlert(String targetEmail, String hospitalName, String disasterType, Double distanceKm, Integer casualties) {
        String subject = "🏥 HOSPITAL SURGE STANDBY: " + hospitalName;
        String alertMessage = String.format(
                "HOSPITAL TRAUMA ADVISORY\n\n" +
                        "Facility: %s\n" +
                        "Incident: %s reported %.1f km from your facility.\n" +
                        "Incoming Casualties: ~%d\n" +
                        "Action Required: Clear trauma bays and reserve emergency blood inventory.\n\n" +
                        "-- Disaster Management Command Center",
                hospitalName, disasterType, distanceKm, (casualties != null ? casualties : 10)
        );

        sendAndLogEmail(targetEmail, subject, alertMessage, "HOSPITAL_STANDBY");
    }

    @Transactional
    public void dispatchLogisticsArrivalAlert(String targetEmail, String itemType, Integer quantity, String vehicleNumber, String warehouseName, String eta) {
        String subject = "🚚 EMERGENCY SUPPLIES EN ROUTE";
        String alertMessage = String.format(
                "LOGISTICS DISPATCH UPDATE\n\n" +
                        "Supplies: %d units of %s\n" +
                        "Source Warehouse: %s\n" +
                        "Transport Vehicle: %s\n" +
                        "Estimated Arrival: %s\n" +
                        "Action: Ensure loading dock clearance.\n\n" +
                        "-- Logistics Command Unit",
                quantity, itemType, warehouseName, vehicleNumber, eta
        );

        sendAndLogEmail(targetEmail, subject, alertMessage, "LOGISTICS_DISPATCH");
    }

    @Transactional
    public int dispatchSafetyCheckInAlert(List<String> targetEmails, String senderName, String statusMessage, Double latitude, Double longitude, String timestamp) {
        String locInfo = (latitude != null && longitude != null)
                ? String.format("Location: Coordinates [%.4f, %.4f]\nMap: https://maps.google.com/?q=%.4f,%.4f\n", latitude, longitude, latitude, longitude)
                : "Location: Not shared by user\n";

        String subject = "🟢 I'm Safe: Safety check-in from " + (senderName != null ? senderName : "a family member");
        String alertMessage = String.format(
                "FAMILY SAFETY CHECK-IN UPDATE\n\n" +
                        "%s has checked in as SAFE on the Disaster Management System (DMS).\n\n" +
                        "Timestamp: %s\n" +
                        "%s" +
                        "Note from %s: %s\n\n" +
                        "-- Disaster Management System Personal Safety Network",
                (senderName != null ? senderName : "Your contact"),
                (timestamp != null ? timestamp : "Recently"),
                locInfo,
                (senderName != null ? senderName : "Contact"),
                (statusMessage != null && !statusMessage.isBlank() ? statusMessage : "I am safe.")
        );

        int sentCount = 0;
        if (targetEmails != null) {
            for (String email : targetEmails) {
                if (email != null && !email.isBlank()) {
                    sendAndLogEmail(email.trim(), subject, alertMessage, "SAFETY_CHECKIN");
                    sentCount++;
                }
            }
        }
        return sentCount;
    }

    @Transactional
    public int dispatchSosEmergencyAlert(List<String> targetEmails, String senderName, String emergencyMessage, Double latitude, Double longitude, String timestamp) {
        String locInfo = (latitude != null && longitude != null)
                ? String.format("Coordinates: [%.5f, %.5f]\nGoogle Maps: https://maps.google.com/?q=%.5f,%.5f\n", latitude, longitude, latitude, longitude)
                : "Coordinates: Location unavailable or permission not granted by device\n";

        String subject = "🚨 URGENT SOS ALERT: " + (senderName != null ? senderName : "Your contact") + " needs immediate emergency assistance!";
        String alertMessage = String.format(
                "CRITICAL SOS EMERGENCY ALERT\n\n" +
                        "EMERGENCY ADVISORY: %s has activated a priority SOS distress alert on the Disaster Management System!\n\n" +
                        "Time: %s\n" +
                        "%s\n" +
                        "Emergency Message:\n\"%s\"\n\n" +
                        "URGENT ACTIONS FOR TRUSTED CONTACTS:\n" +
                        "1. Try calling the contact immediately.\n" +
                        "2. If unreachable and in grave danger, inform emergency authorities (Dial 112 / 108 in India).\n" +
                        "3. Share these verified coordinates with first responders if rescue is needed.\n\n" +
                        "-- Disaster Management System Automated SOS Network",
                (senderName != null ? senderName : "Your contact"),
                (timestamp != null ? timestamp : "Just now"),
                locInfo,
                (emergencyMessage != null && !emergencyMessage.isBlank() ? emergencyMessage : "I need immediate emergency assistance!")
        );

        int sentCount = 0;
        if (targetEmails != null) {
            for (String email : targetEmails) {
                if (email != null && !email.isBlank()) {
                    sendAndLogEmail(email.trim(), subject, alertMessage, "SOS_ALERT");
                    sentCount++;
                }
            }
        }
        return sentCount;
    }

    private void sendAndLogEmail(String recipientEmail, String subject, String body, String type) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(recipientEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(body);

            mailSender.send(mailMessage);

            recordNotificationAudit(recipientEmail, subject, body, type, NotificationStatus.SENT, null);
            log.info("Email alert successfully sent to: {}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", recipientEmail, e.getMessage());
            recordNotificationAudit(recipientEmail, subject, body, type, NotificationStatus.FAILED, e.getMessage());
        }
    }

    private void recordNotificationAudit(String recipient, String subject, String message, String type, NotificationStatus status, String error) {
        NotificationLog logEntry = NotificationLog.builder()
                .recipientEmail(recipient)
                .subject(subject)
                .messageBody(message)
                .alertType(type)
                .status(status)
                .errorMessage(error)
                .build();
        logRepository.save(logEntry);
    }
}