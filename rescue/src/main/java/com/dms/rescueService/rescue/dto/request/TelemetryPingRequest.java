package com.dms.rescueService.rescue.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class TelemetryPingRequest {
    private UUID missionId;
    private Double latitude;
    private Double longitude;
}