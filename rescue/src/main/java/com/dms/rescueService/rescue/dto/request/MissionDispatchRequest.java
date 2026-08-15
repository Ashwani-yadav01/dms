package com.dms.rescueService.rescue.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MissionDispatchRequest {

    @NotNull(message = "Incident ID is required")
    private UUID incidentId;

    @NotNull(message = "Department ID is required")
    private UUID departmentId;

    @NotNull(message = "Leader User ID is required")
    private UUID assignedLeaderId;

    private Integer slaMinutes = 120;
    private String notes;
}