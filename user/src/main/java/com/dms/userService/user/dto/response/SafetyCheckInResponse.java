package com.dms.userService.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyCheckInResponse {
    private UUID id;
    private String clientReferenceId;
    private String status;
    private String message;
    private Double latitude;
    private Double longitude;
    private boolean notifyContacts;
    private int contactsNotifiedCount;
    private String deliveryStatus;
    private Instant createdAt;
}
