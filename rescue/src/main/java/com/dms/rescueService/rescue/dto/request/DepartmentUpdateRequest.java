package com.dms.rescueService.rescue.dto.request;

import com.dms.rescueService.rescue.entity.DepartmentType;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class DepartmentUpdateRequest {

    private String name;
    private DepartmentType type;
    private String jurisdictionCode;
    private Double latitude;
    private Double longitude;
    private String contactPhone;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer totalCapacity;

    private Boolean isAvailable;
}