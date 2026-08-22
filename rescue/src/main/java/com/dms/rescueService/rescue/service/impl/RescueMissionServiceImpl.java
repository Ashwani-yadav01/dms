package com.dms.rescueService.rescue.service.impl;

import com.dms.common.events.RescueMissionStatusUpdatedEvent;
import com.dms.rescueService.rescue.dto.request.MissionActionRequest;
import com.dms.rescueService.rescue.dto.response.RescueMissionResponse;
import com.dms.rescueService.rescue.entity.MissionStatus;
import com.dms.rescueService.rescue.entity.RescueDepartment;
import com.dms.rescueService.rescue.entity.RescueMission;
import com.dms.rescueService.rescue.repository.RescueDepartmentRepository;
import com.dms.rescueService.rescue.repository.RescueMissionRepository;
import com.dms.rescueService.rescue.repository.RescuePersonnelRepository;
import com.dms.rescueService.rescue.service.RedisGeoService;
import com.dms.rescueService.rescue.service.RescueMissionService;
import com.dms.rescueService.rescue.state.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final RescuePersonnelRepository personnelRepository;
    private final RedisGeoService redisGeoService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.rescue-mission-status:rescue-mission-status-topic}")
    private String statusTopic;

    // --- AUTOMATED GPS TELEMETRY PROCESSING ---
    @Override
    @Transactional
    public void processLocationTelemetry(UUID missionId, double latitude, double longitude) {
        RescueMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Rescue Mission not found with ID: " + missionId));

        if (mission.getStatus() == MissionStatus.COMPLETED || mission.getStatus() == MissionStatus.CANCELLED) {
            return;
        }

        UUID unitId = mission.getDepartment().getId();
        UUID incidentId = mission.getIncidentId();

        redisGeoService.updateUnitLocation(unitId, longitude, latitude);

        MissionState currentState = mapStatusToState(mission.getStatus());
        double distanceMeters = redisGeoService.getDistanceToIncidentInMeters(unitId, incidentId);

        MissionState newState = currentState.handleLocationTick(distanceMeters);

        if (newState.getStatus() != mission.getStatus()) {
            executeTransition(mission, newState.getStatus(), "Automated GPS Telemetry transition");
        }
    }

    // --- DEDICATED API ACTION HANDLERS ---

    @Override
    @Transactional
    public RescueMissionResponse completeMission(UUID missionId, MissionActionRequest request) {
        RescueMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Rescue Mission not found with ID: " + missionId));

        MissionState currentState = mapStatusToState(mission.getStatus());
        MissionState newState = currentState.complete(); // Throws IllegalStateException if NOT ON_SCENE

        executeTransition(mission, newState.getStatus(), request.getNotes());
        return mapToResponse(mission);
    }

    @Override
    @Transactional
    public RescueMissionResponse cancelMission(UUID missionId, MissionActionRequest request) {
        RescueMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Rescue Mission not found with ID: " + missionId));

        MissionState currentState = mapStatusToState(mission.getStatus());
        MissionState newState = currentState.cancel(); // Validated via State Pattern

        String notes = request.getReason() != null ? "Cancelled: " + request.getReason() : request.getNotes();
        executeTransition(mission, newState.getStatus(), notes);
        return mapToResponse(mission);
    }

    @Override
    @Transactional
    public RescueMissionResponse escalateMission(UUID missionId, MissionActionRequest request) {
        RescueMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Rescue Mission not found with ID: " + missionId));

        MissionState currentState = mapStatusToState(mission.getStatus());
        MissionState newState = currentState.escalate(); // Validated via State Pattern

        executeTransition(mission, newState.getStatus(), request.getNotes());
        return mapToResponse(mission);
    }

    // --- CENTRAL STATE TRANSITION EXECUTION ---
    private void executeTransition(RescueMission mission, MissionStatus newStatus, String notes) {
        MissionStatus oldStatus = mission.getStatus();
        mission.setStatus(newStatus);

        if (notes != null && !notes.isBlank()) {
            mission.setNotes(notes);
        }

        // Clean spatial RAM cache & free department capacity/assigned personnel on terminal states
        if (newStatus == MissionStatus.COMPLETED || newStatus == MissionStatus.CANCELLED) {
            if (oldStatus != MissionStatus.COMPLETED && oldStatus != MissionStatus.CANCELLED) {
                mission.setCompletedAt(LocalDateTime.now());
                releaseDepartmentCapacity(mission.getDepartment());
                releaseAssignedChief(mission.getAssignedLeaderId());
                redisGeoService.removeSpatialData(mission.getDepartment().getId(), mission.getIncidentId());
            }
        }

        RescueMission updated = missionRepository.save(mission);
        log.info("Transitioned Mission ID: {} from {} to {}", mission.getId(), oldStatus, newStatus);

        publishStatusUpdateEvent(updated);
    }

    private void releaseAssignedChief(UUID assignedLeaderId) {
        if (assignedLeaderId == null) {
            return;
        }

        personnelRepository.findById(assignedLeaderId).ifPresent(chief -> {
            chief.setIsAvailable(true);
            personnelRepository.save(chief);
            log.info("Released Station Chief [{}] (ID: {}) back to available pool.",
                    chief.getFullName(), chief.getId());
        });
    }

    private MissionState mapStatusToState(MissionStatus status) {
        return switch (status) {
            case DISPATCHED -> new DispatchedState();
            case EN_ROUTE -> new EnRouteState();
            case ON_SCENE -> new OnSceneState();
            case ESCALATED -> new EscalatedState();
            case COMPLETED -> new CompletedState();
            case CANCELLED -> new CancelledState();
        };
    }

    private void publishStatusUpdateEvent(RescueMission mission) {
        RescueMissionStatusUpdatedEvent event = RescueMissionStatusUpdatedEvent.builder()
                .missionId(mission.getId())
                .incidentId(mission.getIncidentId())
                .departmentId(mission.getDepartment().getId())
                .status(mission.getStatus())
                .notes(mission.getNotes())
                .updatedAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(statusTopic, mission.getIncidentId().toString(), event);
        log.info("Published RescueMissionStatusUpdatedEvent to topic [{}] for Incident ID: {}, Status: {}",
                statusTopic, mission.getIncidentId(), mission.getStatus());
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