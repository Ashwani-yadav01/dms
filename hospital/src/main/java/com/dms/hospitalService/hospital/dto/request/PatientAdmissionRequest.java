package com.dms.hospitalService.hospital.dto.request;

import com.dms.hospitalService.hospital.entity.TriageLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PatientAdmissionRequest {

    @NotNull(message = "Incident ID is required to track the disaster")
    private UUID incidentId;

    // Optional, as a patient might walk in without a rescue team
    private UUID rescueMissionId;

    @NotNull(message = "Triage level is required")
    private TriageLevel triageLevel;

    @NotNull(message = "ICU requirement flag is required")
    private Boolean requiresIcu;
}