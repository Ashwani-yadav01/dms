package com.dms.hospitalService.hospital.controller;

import com.dms.hospitalService.hospital.dto.request.HospitalBedUpdateRequest;
import com.dms.hospitalService.hospital.dto.request.HospitalCreateRequest;
import com.dms.hospitalService.hospital.dto.request.HospitalUpdateRequest;
import com.dms.hospitalService.hospital.dto.response.HospitalResponse;
import com.dms.hospitalService.hospital.entity.HospitalAudit;
import com.dms.hospitalService.hospital.entity.HospitalStatus;
import com.dms.hospitalService.hospital.entity.MedicalSpeciality;
import com.dms.hospitalService.hospital.service.HospitalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    @PostMapping
    public ResponseEntity<HospitalResponse> createHospital(
            HttpServletRequest request,
            @Valid @RequestBody HospitalCreateRequest hospitalRequest) {
        String role = request.getHeader("X-User-Role");
        if (!isAdminRole(role) && !"HOSPITAL".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        UUID actorId = extractUserId(request);
        if ("HOSPITAL".equals(role)) {
            hospitalRequest.setAdminUserId(actorId);
            hospitalRequest.setStatus(HospitalStatus.PENDING_VERIFICATION);
        } else if (hospitalRequest.getStatus() == null) {
            hospitalRequest.setStatus(HospitalStatus.ACTIVE);
        }
        return new ResponseEntity<>(hospitalService.createHospital(hospitalRequest), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HospitalResponse> getHospitalById(@PathVariable UUID id) {
        return ResponseEntity.ok(hospitalService.getHospitalById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<HospitalResponse> toggleAcceptingPatients(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestParam Boolean isAccepting) {
        if (!canManageHospital(request, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(hospitalService.toggleAcceptingPatients(id, isAccepting));
    }

    @PatchMapping("/{id}/beds")
    public ResponseEntity<HospitalResponse> updateBedCapacity(
            HttpServletRequest request,
            @PathVariable UUID id,
            @Valid @RequestBody HospitalBedUpdateRequest bedRequest) {
        if (!canManageHospital(request, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(hospitalService.updateBedCapacity(id, bedRequest));
    }

    // 🚨 The Smart Routing Endpoint
    @GetMapping("/nearest")
    public ResponseEntity<List<HospitalResponse>> findNearestCapableHospitals(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "50.0") Double radiusKm,
            @RequestParam(required = false) Set<MedicalSpeciality> specialities,
            @RequestParam(defaultValue = "false") Boolean requiresIcu) {

        List<HospitalResponse> nearest = hospitalService.findNearestCapableHospitals(
                latitude, longitude, radiusKm, specialities, requiresIcu);
        return ResponseEntity.ok(nearest);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<HospitalResponse>> searchHospitalsByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(hospitalService.searchHospitalsByName(name, pageable));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHospital(@PathVariable UUID id) {
        hospitalService.deleteHospital(id);
        return ResponseEntity.noContent().build();
    }

    // ========== ADMIN ENDPOINTS ==========
    // These require GOVERNMENT_OFFICIAL or DISTRICT_ADMIN role (validated via X-User-Role header)

    @GetMapping("/admin")
    public ResponseEntity<Page<HospitalResponse>> listHospitals(
            HttpServletRequest request,
            @RequestParam(required = false) HospitalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        String role = request.getHeader("X-User-Role");
        if (!isAdminRole(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(hospitalService.listHospitals(pageable, status));
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<HospitalResponse> getHospitalAdmin(
            HttpServletRequest request,
            @PathVariable UUID id) {

        String role = request.getHeader("X-User-Role");
        if (!isAdminRole(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(hospitalService.getHospitalById(id));
    }

    @PostMapping("/admin")
    public ResponseEntity<HospitalResponse> createHospitalAdmin(
            HttpServletRequest request,
            @Valid @RequestBody HospitalCreateRequest hospitalRequest) {

        String role = request.getHeader("X-User-Role");
        if (!isAdminRole(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return new ResponseEntity<>(hospitalService.createHospitalAdmin(hospitalRequest), HttpStatus.CREATED);
    }

    @PatchMapping("/admin/{id}")
    public ResponseEntity<HospitalResponse> updateHospital(
            HttpServletRequest request,
            @PathVariable UUID id,
            @Valid @RequestBody HospitalUpdateRequest updateRequest) {

        String role = request.getHeader("X-User-Role");
        if (!isAdminRole(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(hospitalService.updateHospital(id, updateRequest));
    }

    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<HospitalResponse> changeHospitalStatus(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestParam HospitalStatus status,
            @RequestParam(required = false) String reason) {

        String role = request.getHeader("X-User-Role");
        if (!isAdminRole(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UUID actorId = extractUserId(request);
        return ResponseEntity.ok(hospitalService.changeHospitalStatus(id, status, reason, actorId, role));
    }

    @PatchMapping("/admin/{id}/verify")
    public ResponseEntity<HospitalResponse> verifyHospital(
            HttpServletRequest request,
            @PathVariable UUID id) {

        String role = request.getHeader("X-User-Role");
        if (!isAdminRole(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UUID actorId = extractUserId(request);
        return ResponseEntity.ok(hospitalService.verifyHospital(id, actorId, role));
    }

    @PatchMapping("/admin/{id}/reject")
    public ResponseEntity<HospitalResponse> rejectHospital(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "Rejected by authorized official") String reason) {

        String role = request.getHeader("X-User-Role");
        if (!isAdminRole(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UUID actorId = extractUserId(request);
        return ResponseEntity.ok(hospitalService.rejectHospital(id, reason, actorId, role));
    }

    @GetMapping("/admin/{id}/audits")
    public ResponseEntity<List<HospitalAudit>> getHospitalAudits(
            HttpServletRequest request,
            @PathVariable UUID id) {

        String role = request.getHeader("X-User-Role");
        if (!isAdminRole(role) && !canManageHospital(request, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(hospitalService.getHospitalAudits(id));
    }

    private boolean isAdminRole(String role) {
        return "GOVERNMENT_OFFICIAL".equals(role) || "DISTRICT_ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }

    private boolean canManageHospital(HttpServletRequest request, UUID hospitalId) {
        String role = request.getHeader("X-User-Role");
        if (isAdminRole(role)) {
            return true;
        }
        if ("HOSPITAL".equals(role)) {
            UUID actorId = extractUserId(request);
            if (actorId == null) return false;
            try {
                HospitalResponse h = hospitalService.getHospitalById(hospitalId);
                return h.getAdminUserId() == null || h.getAdminUserId().equals(actorId);
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