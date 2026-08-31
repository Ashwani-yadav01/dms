package com.dms.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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