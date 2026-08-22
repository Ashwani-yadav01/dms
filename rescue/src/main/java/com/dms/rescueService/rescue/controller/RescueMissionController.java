package com.dms.rescueService.rescue.controller;

import com.dms.rescueService.rescue.dto.request.MissionActionRequest;
import com.dms.rescueService.rescue.dto.response.RescueMissionResponse;
import com.dms.rescueService.rescue.entity.RescueMission;
import com.dms.rescueService.rescue.service.RedisGeoService;
import com.dms.rescueService.rescue.service.RescueAssignmentService;
import com.dms.rescueService.rescue.service.RescueMissionService;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rescue/missions")
@RequiredArgsConstructor
public class RescueMissionController {

    private final RescueMissionService missionService;
    private final RedisGeoService redisGeoService;
    private final RescueAssignmentService assignmentService;

    // --- MANUAL DISPATCH ---
    @PostMapping("/dispatch")
    public ResponseEntity<RescueMissionResponse> dispatchManually(
            @RequestParam UUID incidentId,
            @RequestParam UUID departmentId,
            @RequestParam(required = false, defaultValue = "0.0") double lat,
            @RequestParam(required = false, defaultValue = "0.0") double lon) {

        RescueMission mission = assignmentService.assignDepartmentToIncident(incidentId, departmentId, lat, lon);
        return new ResponseEntity<>(mapToResponse(mission), HttpStatus.CREATED);
    }

    // --- READ ENDPOINTS ---
    @GetMapping("/{id}")
    public ResponseEntity<RescueMissionResponse> getMissionById(@PathVariable UUID id) {
        return ResponseEntity.ok(missionService.getMissionById(id));
    }

    @GetMapping("/incident/{incidentId}")
    public ResponseEntity<List<RescueMissionResponse>> getMissionsByIncidentId(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(missionService.getMissionsByIncidentId(incidentId));
    }

    /**
     * High-speed status check reading directly from Redis spatial cache.
     */
    @GetMapping("/{id}/live-status")
    public ResponseEntity<LiveMissionStatusResponse> getLiveMissionStatus(
            @PathVariable UUID id,
            @RequestParam UUID incidentId) {

        String cachedStatus = redisGeoService.getCachedMissionStatus(id);
        double distanceMeters = redisGeoService.getDistanceToIncidentInMeters(id, incidentId);

        // Handle case where team already arrived and spatial data was evicted from Redis
        boolean isOnSceneOrCompleted = "ON_SCENE".equalsIgnoreCase(cachedStatus) || "COMPLETED".equalsIgnoreCase(cachedStatus);
        boolean isWithinGeofence = isOnSceneOrCompleted || (distanceMeters <= 50.0);

        double displayDistance = isOnSceneOrCompleted ? 0.0 : (Math.round(distanceMeters * 100.0) / 100.0);

        LiveMissionStatusResponse response = LiveMissionStatusResponse.builder()
                .missionId(id)
                .incidentId(incidentId)
                .status(cachedStatus)
                .distanceToIncidentMeters(displayDistance)
                .isWithinGeofence(isWithinGeofence)
                .build();

        return ResponseEntity.ok(response);
    }

    // --- DEDICATED ACTION ENDPOINTS ---

    /**
     * Complete a mission (e.g., field unit submits final proof/notes).
     * Enforces State Pattern: Must be ON_SCENE to complete.
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<RescueMissionResponse> completeMission(
            @PathVariable UUID id,
            @Valid @RequestBody MissionActionRequest request) {
        return ResponseEntity.ok(missionService.completeMission(id, request));
    }

    /**
     * Cancel a mission (e.g., dispatcher revokes assignment or false alarm).
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<RescueMissionResponse> cancelMission(
            @PathVariable UUID id,
            @Valid @RequestBody MissionActionRequest request) {
        return ResponseEntity.ok(missionService.cancelMission(id, request));
    }

    /**
     * Escalate a mission (e.g., unit needs backup or hazard escalation).
     */
    @PostMapping("/{id}/escalate")
    public ResponseEntity<RescueMissionResponse> escalateMission(
            @PathVariable UUID id,
            @Valid @RequestBody MissionActionRequest request) {
        return ResponseEntity.ok(missionService.escalateMission(id, request));
    }

    // --- PRIVATE HELPER MAPPER ---
    private RescueMissionResponse mapToResponse(RescueMission m) {
        return RescueMissionResponse.builder()
                .id(m.getId())
                .incidentId(m.getIncidentId())
                .departmentId(m.getDepartment().getId())
                .departmentName(m.getDepartment().getName())
                .assignedLeaderId(m.getAssignedLeaderId())
                .status(m.getStatus())
                .slaMinutes(m.getSlaMinutes())
                .isSlaBreached(m.getIsSlaBreached())
                .dispatchedAt(m.getDispatchedAt())
                .completedAt(m.getCompletedAt())
                .notes(m.getNotes())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    // --- DTO FOR LIVE STATUS ---
    @Data
    @Builder
    public static class LiveMissionStatusResponse {
        private UUID missionId;
        private UUID incidentId;
        private String status;
        private double distanceToIncidentMeters;
        private boolean isWithinGeofence;
    }
}