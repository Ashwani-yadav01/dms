package com.dms.hospitalService.hospital.service;

import com.dms.hospitalService.hospital.dto.request.PatientAdmissionRequest;
import com.dms.hospitalService.hospital.dto.response.PatientAdmissionResponse;

import java.util.UUID;

public interface PatientAdmissionService {
    // Admits patient and safely decrements bed count using pessimistic locking
    PatientAdmissionResponse admitPatient(UUID hospitalId, PatientAdmissionRequest request);

    // Discharges patient and increments bed count
    PatientAdmissionResponse dischargePatient(UUID admissionId);

    // Transfers patient to another facility and increments bed count
    PatientAdmissionResponse transferPatient(UUID admissionId, String transferNotes);

    // Retrieves admissions for a hospital (active only or full history)
    java.util.List<PatientAdmissionResponse> getAdmissionsForHospital(UUID hospitalId, Boolean activeOnly);

    UUID getHospitalIdForAdmission(UUID admissionId);
}