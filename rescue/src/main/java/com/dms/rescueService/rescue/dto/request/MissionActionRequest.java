package com.dms.rescueService.rescue.dto.request;

import lombok.Data;

@Data
public class MissionActionRequest {
    private String notes;
    private String reason;
    private Integer victimsRescued;
}