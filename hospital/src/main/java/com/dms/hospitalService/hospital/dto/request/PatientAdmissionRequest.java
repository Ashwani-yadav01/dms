package com.dms.hospitalService.hospital.dto.request;

import com.dms.hospitalService.hospital.entity.TriageLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PatientAdmissionRequest {

    // Optional for walk-in patients or when mass incident cause is filled directly
    private UUID incidentId;

    // Optional, as a patient might walk in without a rescue team
    private UUID rescueMissionId;

    @NotNull(message = "Triage level is required")
    private TriageLevel triageLevel;

    @NotNull(message = "ICU requirement flag is required")
    private Boolean requiresIcu;

    private String patientName;
    private Integer age;
    private String gender;
    private String contactNumber;
    private String nationalId;
    private String incidentCause;
    private String triageNotes;
}