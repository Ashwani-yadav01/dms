package com.dms.hospitalService.hospital.controller;

import com.dms.hospitalService.hospital.dto.response.HospitalResponse;
import com.dms.hospitalService.hospital.service.HospitalInventoryAlertService;
import com.dms.hospitalService.hospital.service.HospitalService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hospitals")
@RequiredArgsConstructor
public class HospitalInventoryController {

    private final HospitalInventoryAlertService alertService;
    private final HospitalService hospitalService;

    // Example payload for Postman: {"itemType": "BLOOD_O_NEG", "requestedQuantity": 50, "urgencyLevel": "CRITICAL"}
    @PostMapping("/{hospitalId}/inventory/shortage")
    public ResponseEntity<String> declareShortage(
            HttpServletRequest httpRequest,
            @PathVariable UUID hospitalId,
            @RequestBody ShortageRequest request) {

        if (!canManageHospital(httpRequest, hospitalId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden: You are not authorized to declare shortages for this hospital.");
        }

        alertService.triggerShortageAlert(
                hospitalId,
                request.itemType(),
                request.requestedQuantity(),
                request.urgencyLevel()
        );

        return ResponseEntity.accepted().body("Shortage alert broadcasted to Logistics Service.");
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

    // Simple record for the incoming JSON body
    public record ShortageRequest(String itemType, int requestedQuantity, String urgencyLevel) {}
}