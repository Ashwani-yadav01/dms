package com.dms.hospitalService.hospital.dto.request;

import com.dms.hospitalService.hospital.entity.FacilityType;
import com.dms.hospitalService.hospital.entity.MedicalSpeciality;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.Set;

@Data
public class HospitalUpdateRequest {

    private String name;

    @Email
    private String email;

    private String phoneNumber;

    private String address;

    private FacilityType type;

    private Double latitude;

    private Double longitude;

    private Double elevationMeters;

    @Min(0)
    private Integer totalGeneralBeds;

    @Min(0)
    private Integer totalIcuBeds;

    private Set<MedicalSpeciality> specialities;

    private Boolean isAcceptingPatients;
}