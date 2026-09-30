package com.dms.incident.controller;

import com.dms.incident.dto.request.IncidentRequest;
import com.dms.incident.dto.request.IncidentRejectionRequest;
import com.dms.incident.dto.response.IncidentResponse;
import com.dms.incident.dto.response.PhotoUploadResponse;
import com.dms.incident.entity.IncidentStatus;
import com.dms.incident.entity.Severity;
import com.dms.incident.service.CloudinaryImageService;
import com.dms.incident.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final CloudinaryImageService cloudinaryImageService;

    @PostMapping(
            path = "/photos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PhotoUploadResponse> uploadPhoto(
            @RequestPart("photo") MultipartFile photo,
            @RequestHeader("X-User-Id") String userIdHeader
    ) {

        UUID.fromString(userIdHeader);

        String url = cloudinaryImageService.upload(
                photo,
                "reliefops/incidents"
        );

        return ResponseEntity.ok(
                new PhotoUploadResponse(url)
        );
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(
            @Valid @RequestBody IncidentRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String roleHeader
    ) {
        UUID userId = UUID.fromString(userIdHeader);
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
            @RequestHeader("X-User-Id") String userIdHeader,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        UUID userId = UUID.fromString(userIdHeader);
        Page<IncidentResponse> response = incidentService.getIncidentsByUser(userId, pageable);
        return ResponseEntity.ok(response);
    }

    // --- Public Endpoints ---

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
            @RequestParam IncidentStatus status,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestHeader("X-User-Role") String roleHeader
    ) {
        if (!isGovernmentOfficial(roleHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        IncidentResponse response = incidentService.updateIncidentStatus(id, status, photo);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<IncidentResponse> rejectIncident(
            @PathVariable UUID id,
            @Valid @RequestBody IncidentRejectionRequest request,
            @RequestHeader("X-User-Role") String roleHeader
    ) {
        if (!isGovernmentOfficial(roleHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(incidentService.rejectIncident(id, request.getReason()));
    }

    @PostMapping(path = "/{id}/resolve", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IncidentResponse> resolveIncident(
            @PathVariable UUID id,
            @RequestPart("photo") MultipartFile photo,
            @RequestParam("reason") String reason,
            @RequestHeader("X-User-Role") String roleHeader
    ) {
        if (!isGovernmentOfficial(roleHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(incidentService.resolveIncident(id, photo, reason));
    }

    private boolean isGovernmentOfficial(String role) {
        return "GOVERNMENT_OFFICIAL".equalsIgnoreCase(role);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncidentResponse> updateIncident(
            @PathVariable UUID id,
            @Valid @RequestBody IncidentRequest request,
            @RequestHeader("X-User-Id") String userIdHeader
    ) {
        UUID userId = UUID.fromString(userIdHeader);
        IncidentResponse response = incidentService.updateIncident(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncident(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader
    ) {
        UUID userId = UUID.fromString(userIdHeader);
        incidentService.deleteIncident(id, userId);
        return ResponseEntity.noContent().build();
    }
}
