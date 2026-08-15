package com.dms.rescueService.rescue.controller;

import com.dms.rescueService.rescue.dto.request.MissionStatusUpdateRequest;
import com.dms.rescueService.rescue.dto.response.RescueMissionResponse;
import com.dms.rescueService.rescue.entity.MissionStatus;
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

    // --- READ BY ID ---
    @GetMapping("/{id}")
    public ResponseEntity<RescueMissionResponse> getMissionById(@PathVariable UUID id) {
        return ResponseEntity.ok(missionService.getMissionById(id));
    }

    // --- TRANSITION STATUS ---
    @PatchMapping("/{id}/status")
    public ResponseEntity<RescueMissionResponse> updateMissionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody MissionStatusUpdateRequest request) {
        return ResponseEntity.ok(missionService.updateMissionStatus(id, request));
    }

    // --- SEARCH BY INCIDENT ---
    @GetMapping("/incident/{incidentId}")
    public ResponseEntity<List<RescueMissionResponse>> getMissionsByIncidentId(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(missionService.getMissionsByIncidentId(incidentId));
    }

    // --- SEARCH BY DEPARTMENT ---
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<RescueMissionResponse>> getMissionsByDepartmentId(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(missionService.getMissionsByDepartmentId(departmentId));
    }

    // --- FILTER BY STATUS ---
    @GetMapping("/status/{status}")
    public ResponseEntity<List<RescueMissionResponse>> getMissionsByStatus(@PathVariable MissionStatus status) {
        return ResponseEntity.ok(missionService.getMissionsByStatus(status));
    }
}