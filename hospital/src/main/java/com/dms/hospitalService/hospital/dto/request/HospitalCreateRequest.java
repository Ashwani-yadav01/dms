package com.dms.hospitalService.hospital.dto.request;

import com.dms.hospitalService.hospital.entity.FacilityType;
import com.dms.hospitalService.hospital.entity.MedicalSpeciality;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class HospitalCreateRequest {

    @NotBlank(message = "Hospital name is required")
    private String name;

    @NotNull(message = "Facility type is required")
    private FacilityType type;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotNull(message = "Elevation is required for flood-safe routing")
    private Double elevationMeters;

    @NotNull(message = "Total general beds required")
    @Min(0)
    private Integer totalGeneralBeds;

    @NotNull(message = "Total ICU beds required")
    @Min(0)
    private Integer totalIcuBeds;

    // Optional list of specialities (e.g. BURN_UNIT, TRAUMA_CENTER)
    private Set<MedicalSpeciality> specialities;
}