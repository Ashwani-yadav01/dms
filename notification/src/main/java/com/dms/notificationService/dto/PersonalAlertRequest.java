package com.dms.notificationService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalAlertRequest {
    private List<String> recipientEmails;
    private String senderName;
    private String alertType; // SAFETY_CHECKIN or SOS_ALERT
    private String message;
    private Double latitude;
    private Double longitude;
    private String timestamp;
}
