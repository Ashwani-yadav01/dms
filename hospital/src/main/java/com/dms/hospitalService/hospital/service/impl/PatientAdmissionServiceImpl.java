package com.dms.hospitalService.hospital.service.impl;

import com.dms.hospitalService.hospital.dto.request.PatientAdmissionRequest;
import com.dms.hospitalService.hospital.dto.response.PatientAdmissionResponse;
import com.dms.hospitalService.hospital.entity.Hospital;
import com.dms.hospitalService.hospital.entity.PatientAdmission;
import com.dms.hospitalService.hospital.repository.HospitalRepository;
import com.dms.hospitalService.hospital.repository.PatientAdmissionRepository;
import com.dms.hospitalService.hospital.service.PatientAdmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientAdmissionServiceImpl implements PatientAdmissionService {

    private final PatientAdmissionRepository admissionRepository;
    private final HospitalRepository hospitalRepository;

    @Override
    @Transactional
    public PatientAdmissionResponse admitPatient(UUID hospitalId, PatientAdmissionRequest request) {
        // 🚨 PESSIMISTIC WRITE LOCK: Locks this hospital row until transaction completes
        Hospital hospital = hospitalRepository.findByIdForUpdate(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));

        if (!hospital.getIsAcceptingPatients()) {
            throw new IllegalStateException("Hospital is currently not accepting patients.");
        }

        // Check & Decrement Capacity
        if (request.getRequiresIcu()) {
            if (hospital.getAvailableIcuBeds() <= 0) throw new IllegalStateException("No ICU beds available");
            hospital.setAvailableIcuBeds(hospital.getAvailableIcuBeds() - 1);
        } else {
            if (hospital.getAvailableGeneralBeds() <= 0) throw new IllegalStateException("No general beds available");
            hospital.setAvailableGeneralBeds(hospital.getAvailableGeneralBeds() - 1);
        }

        hospital.setBedLastUpdatedAt(LocalDateTime.now());
        hospitalRepository.save(hospital);

        PatientAdmission admission = PatientAdmission.builder()
                .incidentId(request.getIncidentId())
                .rescueMissionId(request.getRescueMissionId())
                .hospital(hospital)
                .triageLevel(request.getTriageLevel())
                .requiresIcu(request.getRequiresIcu())
                .patientName(request.getPatientName() != null && !request.getPatientName().isBlank() ? request.getPatientName().trim() : "Unidentified Victim")
                .age(request.getAge())
                .gender(request.getGender())
                .contactNumber(request.getContactNumber() != null && !request.getContactNumber().isBlank() ? request.getContactNumber().trim() : null)
                .nationalId(request.getNationalId() != null && !request.getNationalId().isBlank() ? request.getNationalId().trim() : null)
                .incidentCause(request.getIncidentCause() != null && !request.getIncidentCause().isBlank() ? request.getIncidentCause().trim() : null)
                .triageNotes(request.getTriageNotes())
                .status(com.dms.hospitalService.hospital.entity.AdmissionStatus.ADMITTED)
                .build();

        admission = admissionRepository.save(admission);
        log.info("Admitted patient {} for incident {} to hospital {}", admission.getPatientName(), request.getIncidentId(), hospital.getName());

        return mapToResponse(admission);
    }

    @Override
    @Transactional
    public PatientAdmissionResponse dischargePatient(UUID admissionId) {
        PatientAdmission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new IllegalArgumentException("Admission not found"));

        if (admission.getDischargedAt() != null) {
            throw new IllegalStateException("Patient is already discharged or transferred");
        }

        // Lock hospital to return the bed safely
        Hospital hospital = hospitalRepository.findByIdForUpdate(admission.getHospital().getId())
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));

        if (admission.getRequiresIcu()) {
            hospital.setAvailableIcuBeds(hospital.getAvailableIcuBeds() + 1);
        } else {
            hospital.setAvailableGeneralBeds(hospital.getAvailableGeneralBeds() + 1);
        }

        hospital.setBedLastUpdatedAt(LocalDateTime.now());
        hospitalRepository.save(hospital);

        admission.setDischargedAt(LocalDateTime.now());
        admission.setStatus(com.dms.hospitalService.hospital.entity.AdmissionStatus.DISCHARGED);
        admission = admissionRepository.save(admission);

        log.info("Discharged patient {} from hospital {}", admission.getPatientName(), hospital.getName());
        return mapToResponse(admission);
    }

    @Override
    @Transactional
    public PatientAdmissionResponse transferPatient(UUID admissionId, String transferNotes) {
        PatientAdmission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new IllegalArgumentException("Admission not found"));

        if (admission.getDischargedAt() != null) {
            throw new IllegalStateException("Patient is already discharged or transferred");
        }

        // Release the bed at current hospital using pessimistic lock
        Hospital hospital = hospitalRepository.findByIdForUpdate(admission.getHospital().getId())
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));

        if (admission.getRequiresIcu()) {
            hospital.setAvailableIcuBeds(hospital.getAvailableIcuBeds() + 1);
        } else {
            hospital.setAvailableGeneralBeds(hospital.getAvailableGeneralBeds() + 1);
        }

        hospital.setBedLastUpdatedAt(LocalDateTime.now());
        hospitalRepository.save(hospital);

        admission.setDischargedAt(LocalDateTime.now());
        admission.setStatus(com.dms.hospitalService.hospital.entity.AdmissionStatus.TRANSFERRED);
        if (transferNotes != null && !transferNotes.isBlank()) {
            String existing = admission.getTriageNotes() != null ? admission.getTriageNotes() + " | " : "";
            admission.setTriageNotes(existing + "Transfer Notes: " + transferNotes.trim());
        }
        admission = admissionRepository.save(admission);

        log.info("Transferred patient {} from hospital {}", admission.getPatientName(), hospital.getName());
        return mapToResponse(admission);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<PatientAdmissionResponse> getAdmissionsForHospital(UUID hospitalId, Boolean activeOnly) {
        java.util.List<PatientAdmission> admissions;
        if (Boolean.TRUE.equals(activeOnly)) {
            admissions = admissionRepository.findByHospitalIdAndDischargedAtIsNull(hospitalId);
        } else {
            admissions = admissionRepository.findByHospitalIdOrderByAdmittedAtDesc(hospitalId);
        }
        return admissions.stream().map(this::mapToResponse).toList();
    }

    private PatientAdmissionResponse mapToResponse(PatientAdmission a) {
        return PatientAdmissionResponse.builder()
                .id(a.getId())
                .incidentId(a.getIncidentId())
                .rescueMissionId(a.getRescueMissionId())
                .hospitalId(a.getHospital().getId())
                .hospitalName(a.getHospital().getName())
                .triageLevel(a.getTriageLevel())
                .requiresIcu(a.getRequiresIcu())
                .patientName(a.getPatientName())
                .age(a.getAge())
                .gender(a.getGender())
                .contactNumber(a.getContactNumber())
                .nationalId(a.getNationalId())
                .incidentCause(a.getIncidentCause())
                .triageNotes(a.getTriageNotes())
                .status(a.getStatus())
                .admittedAt(a.getAdmittedAt())
                .dischargedAt(a.getDischargedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getHospitalIdForAdmission(UUID admissionId) {
        return admissionRepository.findById(admissionId)
                .map(admission -> admission.getHospital().getId())
                .orElse(null);
    }
}