package com.dms.hospitalService.hospital.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalBedUpdateRequest {

    @NotNull(message = "Available general beds is required")
    @Min(value = 0, message = "Available general beds cannot be negative")
    private Integer availableGeneralBeds;

    @NotNull(message = "Available ICU beds is required")
    @Min(value = 0, message = "Available ICU beds cannot be negative")
    private Integer availableIcuBeds;
}
