package com.dms.rescueService.rescue.controller;

import com.dms.rescueService.rescue.dto.request.MissionActionRequest;
import com.dms.rescueService.rescue.dto.response.RescueMissionResponse;
import com.dms.rescueService.rescue.service.RescueMissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rescue/missions")
@RequiredArgsConstructor
public class RescueMissionController {

    private final RescueMissionService missionService;

    // --- READ ENDPOINTS ---
    @GetMapping("/{id}")
    public ResponseEntity<RescueMissionResponse> getMissionById(@PathVariable UUID id) {
        return ResponseEntity.ok(missionService.getMissionById(id));
    }

    @GetMapping("/incident/{incidentId}")
    public ResponseEntity<List<RescueMissionResponse>> getMissionsByIncidentId(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(missionService.getMissionsByIncidentId(incidentId));
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
}