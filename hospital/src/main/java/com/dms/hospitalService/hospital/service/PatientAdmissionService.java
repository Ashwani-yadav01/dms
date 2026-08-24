package com.dms.hospitalService.hospital.service;

import com.dms.hospitalService.hospital.dto.request.PatientAdmissionRequest;
import com.dms.hospitalService.hospital.dto.response.PatientAdmissionResponse;

import java.util.UUID;

public interface PatientAdmissionService {
    // Admits patient and safely decrements bed count using pessimistic locking
    PatientAdmissionResponse admitPatient(UUID hospitalId, PatientAdmissionRequest request);

    // Discharges patient and increments bed count
    PatientAdmissionResponse dischargePatient(UUID admissionId);
}