package com.dms.rescueService.rescue.dto.response;

import com.dms.rescueService.rescue.entity.MissionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MissionResponse {
    private UUID id;
    private UUID incidentId;
    private UUID departmentId;
    private String departmentName;
    private UUID assignedLeaderId;
    private MissionStatus status;
    private Integer slaMinutes;
    private Boolean isSlaBreached;
    private LocalDateTime dispatchedAt;
    private LocalDateTime completedAt;
    private String notes;
}