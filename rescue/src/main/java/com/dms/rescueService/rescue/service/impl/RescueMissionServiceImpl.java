package com.dms.rescueService.rescue.service.impl;

import com.dms.rescueService.rescue.dto.request.MissionStatusUpdateRequest;
import com.dms.rescueService.rescue.dto.response.RescueMissionResponse;
import com.dms.rescueService.rescue.entity.MissionStatus;
import com.dms.rescueService.rescue.entity.RescueDepartment;
import com.dms.rescueService.rescue.entity.RescueMission;
import com.dms.rescueService.rescue.repository.RescueDepartmentRepository;
import com.dms.rescueService.rescue.repository.RescueMissionRepository;
import com.dms.rescueService.rescue.service.RescueMissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RescueMissionServiceImpl implements RescueMissionService {

    private final RescueMissionRepository missionRepository;
    private final RescueDepartmentRepository departmentRepository;

    @Override
    @Transactional
    public RescueMissionResponse updateMissionStatus(UUID missionId, MissionStatusUpdateRequest request) {
        RescueMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Rescue Mission not found with ID: " + missionId));

        MissionStatus oldStatus = mission.getStatus();
        MissionStatus newStatus = request.getStatus();

        if (oldStatus == newStatus) {
            return mapToResponse(mission);
        }

        mission.setStatus(newStatus);
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            mission.setNotes(request.getNotes().trim());
        }

        // Handle terminal states: free up department capacity
        if (newStatus == MissionStatus.COMPLETED || newStatus == MissionStatus.CANCELLED) {
            if (oldStatus != MissionStatus.COMPLETED && oldStatus != MissionStatus.CANCELLED) {
                mission.setCompletedAt(LocalDateTime.now());
                releaseDepartmentCapacity(mission.getDepartment());
            }
        }

        RescueMission updated = missionRepository.save(mission);
        log.info("Transitioned Mission ID: {} status from {} to {}", missionId, oldStatus, newStatus);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public RescueMissionResponse getMissionById(UUID missionId) {
        return missionRepository.findById(missionId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Rescue Mission not found with ID: " + missionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RescueMissionResponse> getMissionsByIncidentId(UUID incidentId) {
        return missionRepository.findByIncidentId(incidentId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RescueMissionResponse> getMissionsByDepartmentId(UUID departmentId) {
        return missionRepository.findByDepartmentId(departmentId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RescueMissionResponse> getMissionsByStatus(MissionStatus status) {
        return missionRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void releaseDepartmentCapacity(RescueDepartment department) {
        int currentCount = department.getActiveMissionsCount();
        if (currentCount > 0) {
            department.setActiveMissionsCount(currentCount - 1);
        }
        if (!department.getIsAvailable() && department.getActiveMissionsCount() < department.getTotalCapacity()) {
            department.setIsAvailable(true);
        }
        departmentRepository.save(department);
        log.info("Released capacity for Department ID: {}. Active missions remaining: {}",
                department.getId(), department.getActiveMissionsCount());
    }

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
}