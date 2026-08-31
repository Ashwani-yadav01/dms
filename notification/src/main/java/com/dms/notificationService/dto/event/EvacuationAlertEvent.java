package com.dms.notificationService.dto.event;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvacuationAlertEvent {
    private UUID incidentId;
    private String disasterType;
    private String hazardZoneName;
    private Double radiusKm;
    private List<String> targetEmails;
    private String evacuationInstructions;
    private LocalDateTime timestamp;
}