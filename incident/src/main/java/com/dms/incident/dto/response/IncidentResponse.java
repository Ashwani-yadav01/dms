package com.dms.incident.dto.response;

import com.dms.incident.entity.IncidentStatus;
import com.dms.incident.entity.Severity;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class IncidentResponse {
    private UUID id;
    private String title;
    private String description;
    private Double latitude;
    private Double longitude;
    private Severity severity;
    private IncidentStatus status;
    private UUID reportedBy;
    private LocalDateTime createdAt;
}