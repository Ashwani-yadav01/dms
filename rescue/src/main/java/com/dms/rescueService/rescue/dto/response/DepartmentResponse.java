package com.dms.rescueService.rescue.dto.response;

import com.dms.rescueService.rescue.entity.DepartmentType;
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
public class DepartmentResponse {
    private UUID id;
    private String name;
    private DepartmentType type;
    private String jurisdictionCode;
    private Double latitude;
    private Double longitude;
    private String contactPhone;
    private Integer totalCapacity;
    private Integer activeMissionsCount;
    private Boolean isAvailable;
    private UUID stationChiefId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}