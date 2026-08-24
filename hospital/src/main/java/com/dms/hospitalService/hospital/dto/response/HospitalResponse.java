package com.dms.hospitalService.hospital.dto.response;

import com.dms.hospitalService.hospital.entity.FacilityType;
import com.dms.hospitalService.hospital.entity.MedicalSpeciality;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class HospitalResponse {
    private UUID id;
    private String name;
    private FacilityType type;
    private Double latitude;
    private Double longitude;
    private Double elevationMeters;
    private Integer availableGeneralBeds;
    private Integer availableIcuBeds;
    private Set<MedicalSpeciality> specialities;
    private Boolean isAcceptingPatients;
    private LocalDateTime updatedAt;
}