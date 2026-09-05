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
    public ResponseEntity<List<NotificationLog>> getAllLogs() {
        return ResponseEntity.ok(logRepository.findAll());
    }

    @GetMapping("/logs/recipient")
    public ResponseEntity<List<NotificationLog>> getLogsByEmail(@RequestParam String email) {
        return ResponseEntity.ok(logRepository.findByRecipientEmail(email));
    }

    @PostMapping("/test-email")
    public ResponseEntity<String> sendTestEmail(
            @RequestParam String email,
            @RequestParam(defaultValue = "Flash Flood") String disasterType,
            @RequestParam(defaultValue = "Sector 4B") String zone) {

        dispatchService.dispatchMassEvacuationAlert(
                List.of(email),
                disasterType,
                zone,
                "Move to designated high-ground relief centers immediately."
        );
        return ResponseEntity.ok("Emergency test email dispatched to " + email);
    }


}