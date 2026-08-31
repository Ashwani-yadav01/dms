package com.dms.notificationService.dto.event;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalSurgeStandbyEvent {
    private UUID incidentId;
    private String hospitalEmail;
    private String hospitalName;
    private String disasterType;
    private Double distanceKm;
    private Integer estimatedCasualties;
    private LocalDateTime timestamp;
}