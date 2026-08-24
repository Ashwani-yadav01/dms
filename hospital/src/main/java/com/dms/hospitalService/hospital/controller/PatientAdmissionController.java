package com.dms.hospitalService.hospital.controller;

import com.dms.hospitalService.hospital.dto.request.PatientAdmissionRequest;
import com.dms.hospitalService.hospital.dto.response.PatientAdmissionResponse;
import com.dms.hospitalService.hospital.service.PatientAdmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admissions")
@RequiredArgsConstructor
public class PatientAdmissionController {

    private final PatientAdmissionService admissionService;

    @PostMapping("/hospitals/{hospitalId}")
    public ResponseEntity<PatientAdmissionResponse> admitPatient(
            @PathVariable UUID hospitalId,
            @Valid @RequestBody PatientAdmissionRequest request) {
        return new ResponseEntity<>(admissionService.admitPatient(hospitalId, request), HttpStatus.CREATED);
    }

    @PatchMapping("/{admissionId}/discharge")
    public ResponseEntity<PatientAdmissionResponse> dischargePatient(@PathVariable UUID admissionId) {
        return ResponseEntity.ok(admissionService.dischargePatient(admissionId));
    }
}