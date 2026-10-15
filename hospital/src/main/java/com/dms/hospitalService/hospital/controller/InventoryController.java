package com.dms.hospitalService.hospital.controller;

import com.dms.hospitalService.hospital.dto.request.InventoryCreateRequest;
import com.dms.hospitalService.hospital.dto.request.InventoryUpdateRequest;
import com.dms.hospitalService.hospital.dto.response.HospitalResponse;
import com.dms.hospitalService.hospital.dto.response.InventoryResponse;
import com.dms.hospitalService.hospital.service.HospitalService;
import com.dms.hospitalService.hospital.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hospitals")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final HospitalService hospitalService;

    @PostMapping("/{hospitalId}/inventory")
    public ResponseEntity<InventoryResponse> initializeInventory(
            HttpServletRequest httpRequest,
            @PathVariable UUID hospitalId,
            @Valid @RequestBody InventoryCreateRequest request) {
        if (!canManageHospital(httpRequest, hospitalId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return new ResponseEntity<>(inventoryService.initializeInventory(hospitalId, request), HttpStatus.CREATED);
    }

    @PatchMapping("/{hospitalId}/inventory")
    public ResponseEntity<InventoryResponse> updateInventory(
            HttpServletRequest httpRequest,
            @PathVariable UUID hospitalId,
            @Valid @RequestBody InventoryUpdateRequest request) {
        if (!canManageHospital(httpRequest, hospitalId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(inventoryService.updateInventoryQuantity(hospitalId, request));
    }

    @PutMapping("/{hospitalId}/inventory/batch")
    public ResponseEntity<List<InventoryResponse>> batchUpdateInventory(
            HttpServletRequest httpRequest,
            @PathVariable UUID hospitalId,
            @Valid @RequestBody List<InventoryUpdateRequest> updates) {
        if (!canManageHospital(httpRequest, hospitalId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(inventoryService.batchUpdateInventory(hospitalId, updates));
    }

    @GetMapping("/{hospitalId}/inventory")
    public ResponseEntity<List<InventoryResponse>> getHospitalInventory(@PathVariable UUID hospitalId) {
        return ResponseEntity.ok(inventoryService.getHospitalInventory(hospitalId));
    }

    // AI Forecaster / Supply Chain trigger endpoint
    @GetMapping("/inventory/shortages")
    public ResponseEntity<List<InventoryResponse>> getCriticalShortages() {
        return ResponseEntity.ok(inventoryService.getCriticalShortages());
    }

    @DeleteMapping("/inventory/{id}")
    public ResponseEntity<Void> deleteInventoryItem(
            HttpServletRequest httpRequest,
            @PathVariable UUID id) {
        UUID hospitalId = inventoryService.getHospitalIdForInventoryItem(id);
        if (hospitalId == null || !canManageHospital(httpRequest, hospitalId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        inventoryService.deleteInventoryItem(id);
        return ResponseEntity.noContent().build();
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