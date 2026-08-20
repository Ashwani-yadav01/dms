package com.dms.rescueService.rescue.service;

import com.dms.rescueService.rescue.dto.request.MissionActionRequest;
import com.dms.rescueService.rescue.dto.response.RescueMissionResponse;
import com.dms.rescueService.rescue.entity.MissionStatus;

import java.util.List;
import java.util.UUID;

public interface RescueMissionService {

    void processLocationTelemetry(UUID missionId, double latitude, double longitude);

    RescueMissionResponse completeMission(UUID missionId, MissionActionRequest request);

    RescueMissionResponse cancelMission(UUID missionId, MissionActionRequest request);

    RescueMissionResponse escalateMission(UUID missionId, MissionActionRequest request);

    RescueMissionResponse getMissionById(UUID missionId);

    List<RescueMissionResponse> getMissionsByIncidentId(UUID incidentId);

    List<RescueMissionResponse> getMissionsByDepartmentId(UUID departmentId);

    List<RescueMissionResponse> getMissionsByStatus(MissionStatus status);
}