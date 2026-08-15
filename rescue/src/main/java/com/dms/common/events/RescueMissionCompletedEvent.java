package com.dms.common.events;

import com.dms.rescueService.rescue.entity.MissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescueMissionCompletedEvent implements Serializable {

    private UUID missionId;
    private UUID incidentId;
    private UUID departmentId;
    private MissionStatus status;
    private LocalDateTime completedAt;
    private String notes;
}