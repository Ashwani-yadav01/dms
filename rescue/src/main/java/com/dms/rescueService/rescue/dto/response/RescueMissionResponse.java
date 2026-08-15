package com.dms.rescueService.rescue.dto.response;

import com.dms.rescueService.rescue.entity.MissionStatus;
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
public class RescueMissionResponse {

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}