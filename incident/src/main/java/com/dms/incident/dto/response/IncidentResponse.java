package com.dms.incident.dto.response;

import com.dms.incident.entity.IncidentStatus;
import com.dms.incident.entity.IncidentType;
import com.dms.incident.entity.Severity;
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
public class IncidentResponse {
    private UUID id;
    private String title;
    private String description;
    private Double latitude;
    private Double longitude;
    private String imageUrl;
    private String resolutionPhotoUrl;
    private String rejectionReason;
    private String resolutionReason;
    private Severity severity;
    private IncidentType incidentType;
    private IncidentStatus status;
    private UUID reportedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
