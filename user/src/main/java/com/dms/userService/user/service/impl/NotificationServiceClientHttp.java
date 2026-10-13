package com.dms.userService.user.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class NotificationServiceClientHttp {

    private final RestClient client;

    public NotificationServiceClientHttp(RestClient.Builder builder,
                                        @Value("${application.notification-service.url:http://localhost:8086}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    public int dispatchSafetyCheckIn(List<String> recipientEmails, String senderName, String message, Double latitude, Double longitude, String timestamp) {
        if (recipientEmails == null || recipientEmails.isEmpty()) return 0;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("recipientEmails", recipientEmails);
            payload.put("senderName", senderName != null ? senderName : "User");
            payload.put("alertType", "SAFETY_CHECKIN");
            payload.put("message", message != null ? message : "");
            payload.put("timestamp", timestamp != null ? timestamp : "");
            if (latitude != null) payload.put("latitude", latitude);
            if (longitude != null) payload.put("longitude", longitude);

            Integer count = client.post()
                    .uri("/api/v1/notifications/personal/safety-checkin")
                    .body(payload)
                    .retrieve()
                    .body(Integer.class);
            return count != null ? count : recipientEmails.size();
        } catch (Exception e) {
            log.warn("Failed to dispatch safety check-in notification via notification-service: {}", e.getMessage());
            return 0;
        }
    }

    public int dispatchSosAlert(List<String> recipientEmails, String senderName, String message, Double latitude, Double longitude, String timestamp) {
        if (recipientEmails == null || recipientEmails.isEmpty()) return 0;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("recipientEmails", recipientEmails);
            payload.put("senderName", senderName != null ? senderName : "User in Distress");
            payload.put("alertType", "SOS_ALERT");
            payload.put("message", message != null ? message : "");
            payload.put("timestamp", timestamp != null ? timestamp : "");
            if (latitude != null) payload.put("latitude", latitude);
            if (longitude != null) payload.put("longitude", longitude);

            Integer count = client.post()
                    .uri("/api/v1/notifications/personal/sos")
                    .body(payload)
                    .retrieve()
                    .body(Integer.class);
            return count != null ? count : recipientEmails.size();
        } catch (Exception e) {
            log.warn("Failed to dispatch SOS alert notification via notification-service: {}", e.getMessage());
            return 0;
        }
    }
}
