package com.dms.hospitalService.hospital.controller;

import com.dms.hospitalService.hospital.dto.request.PatientAdmissionRequest;
import com.dms.hospitalService.hospital.dto.response.HospitalResponse;
import com.dms.hospitalService.hospital.dto.response.PatientAdmissionResponse;
import com.dms.hospitalService.hospital.service.HospitalService;
import com.dms.hospitalService.hospital.service.PatientAdmissionService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final HospitalService hospitalService;

    @PostMapping("/hospitals/{hospitalId}")
    public ResponseEntity<PatientAdmissionResponse> admitPatient(
            HttpServletRequest httpRequest,
            @PathVariable UUID hospitalId,
            @Valid @RequestBody PatientAdmissionRequest request) {

        if (!canManageHospital(httpRequest, hospitalId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return new ResponseEntity<>(admissionService.admitPatient(hospitalId, request), HttpStatus.CREATED);
    }

    @PatchMapping("/{admissionId}/discharge")
    public ResponseEntity<PatientAdmissionResponse> dischargePatient(
            HttpServletRequest httpRequest,
            @PathVariable UUID admissionId) {

        UUID hospitalId = admissionService.getHospitalIdForAdmission(admissionId);
        if (hospitalId == null || !canManageHospital(httpRequest, hospitalId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(admissionService.dischargePatient(admissionId));
    }

    private boolean isAuthorizedHospitalAdmin(HttpServletRequest request) {
        String role = request.getHeader("X-User-Role");
        if ("SUPER_ADMIN".equals(role) || "DISTRICT_ADMIN".equals(role)) {
            return true;
        }
        if ("GOVERNMENT_OFFICIAL".equals(role)) {
            String deptCategory = request.getHeader("X-User-Department-Category");
            return "MEDICAL_RESPONSE".equals(deptCategory);
        }
        return false;
    }

    private boolean canManageHospital(HttpServletRequest request, UUID hospitalId) {
        if (isAuthorizedHospitalAdmin(request)) {
            return true;
        }
        String role = request.getHeader("X-User-Role");
        if ("HOSPITAL".equals(role)) {
            UUID actorId = extractUserId(request);
            if (actorId == null) return false;
            try {
                HospitalResponse h = hospitalService.getHospitalById(hospitalId);
                return h.getAdminUserId() != null && h.getAdminUserId().equals(actorId);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private UUID extractUserId(HttpServletRequest request) {
        String idHeader = request.getHeader("X-User-Id");
        if (idHeader != null && !idHeader.isBlank()) {
            try {
                return UUID.fromString(idHeader);
            } catch (Exception ignored) {}
        }
        return null;
    }
}