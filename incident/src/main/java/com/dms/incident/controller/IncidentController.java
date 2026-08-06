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
        // TODO: Implement method
        return null;
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncidentById(@PathVariable UUID id) {
        // TODO: Implement method
        return null;
    }

    @GetMapping
    public ResponseEntity<Page<IncidentResponse>> getAllIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Severity severity,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        // TODO: Implement method
        return null;
    }

    @GetMapping("/my-reports")
    public ResponseEntity<Page<IncidentResponse>> getMyIncidents(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        // TODO: Implement method
        return null;
    }

    @GetMapping("/active")
    public ResponseEntity<List<IncidentResponse>> getActiveIncidents() {
        // TODO: Implement method
        return null;
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<IncidentResponse>> getNearbyIncidents(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusInKm
    ) {
        // TODO: Implement method
        return null;
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<IncidentResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam IncidentStatus status
    ) {
        // TODO: Implement method
        return null;
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncidentResponse> updateIncident(
            @PathVariable UUID id,
            @Valid @RequestBody IncidentRequest request,
            Authentication authentication
    ) {
        // TODO: Implement method
        return null;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncident(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        // TODO: Implement method
        return null;
    }
}