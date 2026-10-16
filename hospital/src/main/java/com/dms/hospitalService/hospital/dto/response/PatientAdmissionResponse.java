package com.dms.hospitalService.hospital.dto.response;

import com.dms.hospitalService.hospital.entity.TriageLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PatientAdmissionResponse {
    private UUID id;
    private UUID incidentId;
    private UUID rescueMissionId;
    private UUID hospitalId;
    private String hospitalName;
    private TriageLevel triageLevel;
    private Boolean requiresIcu;
    private String patientName;
    private Integer age;
    private String gender;
    private String contactNumber;
    private String nationalId;
    private String incidentCause;
    private String triageNotes;
    private com.dms.hospitalService.hospital.entity.AdmissionStatus status;
    private LocalDateTime admittedAt;
    private LocalDateTime dischargedAt;
}