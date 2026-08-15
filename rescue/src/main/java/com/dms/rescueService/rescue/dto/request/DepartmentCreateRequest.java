package com.dms.rescueService.rescue.dto.request;

import com.dms.rescueService.rescue.entity.DepartmentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DepartmentCreateRequest {

    @NotBlank(message = "Department name is required")
    private String name;

    @NotNull(message = "Department type is required")
    private DepartmentType type;

    @NotBlank(message = "Jurisdiction code is required")
    private String jurisdictionCode;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotBlank(message = "Contact phone is required")
    private String contactPhone;

    @NotNull(message = "Total capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer totalCapacity;
}