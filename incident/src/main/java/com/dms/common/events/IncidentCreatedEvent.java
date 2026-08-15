package com.dms.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentCreatedEvent {
    private UUID incidentId;
    private String title;
    private String description;
    private String incidentType;
    private String severity;
    private Double latitude;
    private Double longitude;
    private UUID reportedBy;
    private LocalDateTime createdAt;
}