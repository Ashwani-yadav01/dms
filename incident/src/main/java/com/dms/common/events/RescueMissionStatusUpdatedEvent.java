package com.dms.common.events;

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
public class RescueMissionStatusUpdatedEvent implements Serializable {

    private UUID missionId;
    private UUID incidentId;
    private UUID departmentId;
    private String status;
    private String notes;
    private LocalDateTime updatedAt;
}