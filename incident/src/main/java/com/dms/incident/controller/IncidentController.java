package com.dms.incident.controller;

import com.dms.incident.dto.request.IncidentRequest;
import com.dms.incident.dto.response.IncidentResponse;
import com.dms.incident.entity.IncidentStatus;
import com.dms.incident.entity.Severity;
import com.dms.incident.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(
            @Valid @RequestBody IncidentRequest request,
            Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        IncidentResponse response = incidentService.createIncident(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncidentById(@PathVariable UUID id) {
        IncidentResponse response = incidentService.getIncidentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<IncidentResponse>> getAllIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Severity severity,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        Page<IncidentResponse> response = incidentService.filterIncidents(status, severity, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-reports")
    public ResponseEntity<Page<IncidentResponse>> getMyIncidents(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        UUID userId = extractUserId(authentication);
        Page<IncidentResponse> response = incidentService.getIncidentsByUser(userId, pageable);
        return ResponseEntity.ok(response);
    }

    // --- Public Endpoints (Matched with /public/** in SecurityConfig) ---

    @GetMapping("/public/active")
    public ResponseEntity<List<IncidentResponse>> getActiveIncidents() {
        List<IncidentResponse> response = incidentService.getActiveIncidents();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/nearby")
    public ResponseEntity<List<IncidentResponse>> getNearbyIncidents(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusInKm
    ) {
        List<IncidentResponse> response = incidentService.getIncidentsInRadius(latitude, longitude, radiusInKm);
        return ResponseEntity.ok(response);
    }

    // --- Incident Status & Management ---

    @PatchMapping("/{id}/status")
    public ResponseEntity<IncidentResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam IncidentStatus status
    ) {
        IncidentResponse response = incidentService.updateIncidentStatus(id, status);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncidentResponse> updateIncident(
            @PathVariable UUID id,
            @Valid @RequestBody IncidentRequest request,
            Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        IncidentResponse response = incidentService.updateIncident(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncident(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        incidentService.deleteIncident(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Extracts and safely converts the principal stored in the Security Context to a UUID.
     * Your JwtAuthenticationFilter stores a java.util.UUID directly as the Principal.
     */
    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("User authentication context is required");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UUID uuid) {
            return uuid;
        }
        if (principal instanceof String str) {
            return UUID.fromString(str);
        }

        throw new IllegalStateException("Unexpected authentication principal type: " + principal.getClass().getName());
    }
}