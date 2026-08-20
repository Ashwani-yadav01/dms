package com.dms.rescueService.rescue.service.impl;

import com.dms.common.events.IncidentCreatedEvent;
import com.dms.common.events.RescueMissionStatusUpdatedEvent;
import com.dms.rescueService.rescue.entity.MissionStatus;
import com.dms.rescueService.rescue.entity.RescueDepartment;
import com.dms.rescueService.rescue.entity.RescueMission;
import com.dms.rescueService.rescue.exception.DepartmentNotFoundException;
import com.dms.rescueService.rescue.repository.RescueDepartmentRepository;
import com.dms.rescueService.rescue.repository.RescueMissionRepository;
import com.dms.rescueService.rescue.service.RedisGeoService;
import com.dms.rescueService.rescue.service.RescueAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RescueAssignmentServiceImpl implements RescueAssignmentService {
// dues  when have not figured out how we will assgin the leader assigned leader is currently reported user
    private final RescueDepartmentRepository departmentRepository;
    private final RescueMissionRepository missionRepository;
    private final RedisGeoService redisGeoService;
    private final KafkaTemplate<String, RescueMissionStatusUpdatedEvent> kafkaTemplate;

    @Value("${app.kafka.topics.rescue-mission-status:rescue-mission-status-topic}")
    private String rescueStatusTopic;

    private static final double SEARCH_RADIUS_KM = 50.0;

    @Override
    @Transactional
    public void autoAssignRescueTeam(IncidentCreatedEvent event) {
        log.info("Starting auto-assignment for Incident ID: [{}] at coordinates ({}, {})",
                event.getIncidentId(), event.getLatitude(), event.getLongitude());

        // 1. Register incident location in Redis GEO for live tracking telemetry
        // Fixed parameter order: latitude first, longitude second
        redisGeoService.registerIncidentLocation(
                event.getIncidentId(),
                event.getLatitude(),
                event.getLongitude()
        );

        // 2. Query DB directly for available departments within radius ordered by distance
        List<RescueDepartment> nearbyDepartments = departmentRepository.findAvailableWithinRadius(
                event.getLatitude(),
                event.getLongitude(),
                SEARCH_RADIUS_KM
        );

        if (nearbyDepartments.isEmpty()) {
            log.warn("Auto-assignment failed for Incident ID: [{}]. No available departments within {} km.",
                    event.getIncidentId(), SEARCH_RADIUS_KM);
            return;
        }

        // Top result is the nearest department with active capacity
        RescueDepartment assignedDept = nearbyDepartments.get(0);

        double distanceKm = calculateDistanceInKm(
                event.getLatitude(), event.getLongitude(),
                assignedDept.getLatitude(), assignedDept.getLongitude()
        );

        // 3. Create and persist Rescue Mission
        RescueMission mission = RescueMission.builder()
                .incidentId(event.getIncidentId())
                .department(assignedDept)
                .assignedLeaderId(event.getReportedBy())
                .status(MissionStatus.DISPATCHED)
                .notes(String.format("Auto-assigned to %s (Dist: %.2f km)", assignedDept.getName(), distanceKm))
                .build();

        RescueMission savedMission = missionRepository.save(mission);

        // 4. Seed initial Redis state
        redisGeoService.cacheMissionStatus(savedMission.getId(), MissionStatus.DISPATCHED.name());

        redisGeoService.updateUnitLocation(
                savedMission.getId(),
                assignedDept.getLatitude(),
                assignedDept.getLongitude()
        );

        // 5. Update Department Capacity & Availability State
        updateDepartmentCapacity(assignedDept);

        // 6. Notify Incident Service via Kafka
        publishStatusEvent(savedMission.getIncidentId(), savedMission.getId(), MissionStatus.DISPATCHED.name(), savedMission.getNotes());

        // Fixed SLF4J logger format specifier from %.2f to {}
        log.info("Successfully assigned Incident ID: [{}] to Department: [{}] (Dist: {} km)",
                event.getIncidentId(), assignedDept.getName(), String.format("%.2f", distanceKm));
    }


    @Transactional
    public RescueMission assignDepartmentToIncident(UUID incidentId, UUID departmentId, double incidentLat, double incidentLon) {
        log.info("Manual assignment requested for Incident ID: [{}] to Department ID: [{}]", incidentId, departmentId);

        RescueDepartment department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Rescue Department not found with ID: " + departmentId));

        if (department.getActiveMissionsCount() >= department.getTotalCapacity()) {
            throw new IllegalStateException("Department " + department.getName() + " is currently at maximum capacity.");
        }

        // Register incident target location in Redis GEO for distance calculations
        redisGeoService.registerIncidentLocation(incidentId, incidentLat, incidentLon);

        RescueMission mission = RescueMission.builder()
                .incidentId(incidentId)
                .department(department)
                .status(MissionStatus.DISPATCHED)
                .notes("Manually assigned by dispatcher operator.")
                .build();

        RescueMission savedMission = missionRepository.save(mission);

        // Seed initial Redis cache state
        redisGeoService.cacheMissionStatus(savedMission.getId(), MissionStatus.DISPATCHED.name());

        redisGeoService.updateUnitLocation(
                savedMission.getId(),
                department.getLatitude(),
                department.getLongitude()
        );

        updateDepartmentCapacity(department);

        publishStatusEvent(savedMission.getIncidentId(), savedMission.getId(), MissionStatus.DISPATCHED.name(), savedMission.getNotes());

        return savedMission;
    }

    @Override
    @Transactional
    public RescueMission assignDepartmentToIncident(UUID incidentId, UUID departmentId) {
        // Fallback overload to satisfy legacy interface calls without coordinates
        return assignDepartmentToIncident(incidentId, departmentId, 0.0, 0.0);
    }

    private void updateDepartmentCapacity(RescueDepartment department) {
        department.setActiveMissionsCount(department.getActiveMissionsCount() + 1);

        if (department.getActiveMissionsCount() >= department.getTotalCapacity()) {
            department.setIsAvailable(false);
            log.info("Department [{}] reached maximum capacity. Marked unavailable.", department.getName());
        }

        departmentRepository.save(department);
    }

    private void publishStatusEvent(UUID incidentId, UUID missionId, String status, String notes) {
        RescueMissionStatusUpdatedEvent statusEvent = RescueMissionStatusUpdatedEvent.builder()
                .incidentId(incidentId)
                .missionId(missionId)
                .status(MissionStatus.fromString(status).orElse(MissionStatus.DISPATCHED))
                .notes(notes)
                .build();

        kafkaTemplate.send(rescueStatusTopic, incidentId.toString(), statusEvent);
    }

    private double calculateDistanceInKm(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }
}