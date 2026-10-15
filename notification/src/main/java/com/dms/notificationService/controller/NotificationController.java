package com.dms.notificationService.controller;

import com.dms.notificationService.entity.NotificationLog;
import com.dms.notificationService.repository.NotificationLogRepository;
import com.dms.notificationService.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationLogRepository logRepository;
    private final NotificationDispatchService dispatchService;

    @GetMapping("/logs")
    public ResponseEntity<List<NotificationLog>> getMyLogs(@RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(logRepository.findByRecipientEmail(email));
    }

    @PostMapping("/test-email")
    public ResponseEntity<String> sendTestEmail(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam String email,
            @RequestParam(defaultValue = "Flash Flood") String disasterType,
            @RequestParam(defaultValue = "Sector 4B") String zone) {

        if (!"SUPER_ADMIN".equals(role) && !"DISTRICT_ADMIN".equals(role)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body("Only administrators can trigger test notification broadcasts");
        }

        dispatchService.dispatchMassEvacuationAlert(
                List.of(email),
                disasterType,
                zone,
                "Move to designated high-ground relief centers immediately."
        );
        return ResponseEntity.ok("Emergency test email dispatched to " + email);
    }

    @PostMapping("/personal/safety-checkin")
    public ResponseEntity<Integer> sendSafetyCheckInAlert(@RequestBody com.dms.notificationService.dto.PersonalAlertRequest request) {
        int sent = dispatchService.dispatchSafetyCheckInAlert(
                request.getRecipientEmails(),
                request.getSenderName(),
                request.getMessage(),
                request.getLatitude(),
                request.getLongitude(),
                request.getTimestamp()
        );
        return ResponseEntity.ok(sent);
    }

    @PostMapping("/personal/sos")
    public ResponseEntity<Integer> sendSosAlert(@RequestBody com.dms.notificationService.dto.PersonalAlertRequest request) {
        int sent = dispatchService.dispatchSosEmergencyAlert(
                request.getRecipientEmails(),
                request.getSenderName(),
                request.getMessage(),
                request.getLatitude(),
                request.getLongitude(),
                request.getTimestamp()
        );
        return ResponseEntity.ok(sent);
    }
}
