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

        hospitalRepository.save(hospital);

        PatientAdmission admission = PatientAdmission.builder()
                .incidentId(request.getIncidentId())
                .rescueMissionId(request.getRescueMissionId())
                .hospital(hospital)
                .triageLevel(request.getTriageLevel())
                .requiresIcu(request.getRequiresIcu())
                .build();

        admission = admissionRepository.save(admission);
        log.info("Admitted patient for incident {} to hospital {}", request.getIncidentId(), hospital.getName());

        return mapToResponse(admission);
    }

    @Override
    @Transactional
    public PatientAdmissionResponse dischargePatient(UUID admissionId) {
        PatientAdmission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new IllegalArgumentException("Admission not found"));

        if (admission.getDischargedAt() != null) {
            throw new IllegalStateException("Patient is already discharged");
        }

        // Lock hospital to return the bed safely
        Hospital hospital = hospitalRepository.findByIdForUpdate(admission.getHospital().getId())
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));

        if (admission.getRequiresIcu()) {
            hospital.setAvailableIcuBeds(hospital.getAvailableIcuBeds() + 1);
        } else {
            hospital.setAvailableGeneralBeds(hospital.getAvailableGeneralBeds() + 1);
        }

        hospitalRepository.save(hospital);

        admission.setDischargedAt(LocalDateTime.now());
        admission = admissionRepository.save(admission);

        return mapToResponse(admission);
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
                .admittedAt(a.getAdmittedAt())
                .dischargedAt(a.getDischargedAt())
                .build();
    }
}