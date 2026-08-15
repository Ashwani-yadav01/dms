package com.dms.rescueService.rescue.service;

import com.dms.rescueService.rescue.dto.request.MissionStatusUpdateRequest;
import com.dms.rescueService.rescue.dto.response.RescueMissionResponse;
import com.dms.rescueService.rescue.entity.MissionStatus;

import java.util.List;
import java.util.UUID;

public interface RescueMissionService {
    RescueMissionResponse updateMissionStatus(UUID missionId, MissionStatusUpdateRequest request);
    RescueMissionResponse getMissionById(UUID missionId);
    List<RescueMissionResponse> getMissionsByIncidentId(UUID incidentId);
    List<RescueMissionResponse> getMissionsByDepartmentId(UUID departmentId);
    List<RescueMissionResponse> getMissionsByStatus(MissionStatus status);
}