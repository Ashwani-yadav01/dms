package com.dms.rescueService.rescue.service.impl;

import com.dms.common.events.IncidentCreatedEvent;
import com.dms.rescueService.rescue.dto.request.DepartmentCreateRequest;
import com.dms.rescueService.rescue.dto.request.DepartmentUpdateRequest;
import com.dms.rescueService.rescue.dto.response.DepartmentResponse;
import com.dms.rescueService.rescue.entity.MissionStatus;
import com.dms.rescueService.rescue.entity.RescueDepartment;
import com.dms.rescueService.rescue.entity.RescueMission;
import com.dms.rescueService.rescue.exception.DepartmentNotFoundException;
import com.dms.rescueService.rescue.repository.RescueDepartmentRepository;
import com.dms.rescueService.rescue.repository.RescueMissionRepository;
import com.dms.rescueService.rescue.service.DepartmentService;
import com.dms.rescueService.rescue.service.RedisGeoService;
import com.dms.rescueService.rescue.service.RescueAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final RescueDepartmentRepository departmentRepository;
    private final RescueMissionRepository missionRepository;

    @Override
    @Transactional
    public void processAutoDispatchForIncident(IncidentCreatedEvent event) {
        log.info("Processing Kafka event for Incident ID: {} at ({}, {})",
                event.getIncidentId(), event.getLatitude(), event.getLongitude());

        double radiusKm = 15.0; // Default spatial search radius limit
        List<RescueDepartment> availableDepts = departmentRepository.findAvailableWithinRadius(
                event.getLatitude(),
                event.getLongitude(),
                radiusKm
        );

        if (availableDepts.isEmpty()) {
            log.warn("No available rescue departments found within {} km for Incident ID: {}",
                    radiusKm, event.getIncidentId());
            return;
        }

        log.info("Found {} available department(s) for Incident ID: {}", availableDepts.size(), event.getIncidentId());

        // Dispatch the nearest available department (first element in ordered result)
        RescueDepartment chosenDept = availableDepts.get(0);

        // Fallback default system UUID if leader ID is not present in the incoming event
        UUID leaderId = event.getAssignedLeaderId() != null
                ? event.getAssignedLeaderId()
                : UUID.fromString("00000000-0000-0000-0000-000000000000");

        RescueMission mission = RescueMission.builder()
                .incidentId(event.getIncidentId())
                .department(chosenDept)
                .assignedLeaderId(leaderId)
                .status(MissionStatus.DISPATCHED)
                .slaMinutes(120)
                .isSlaBreached(false)
                .notes("Auto-dispatched via IncidentCreatedEvent Kafka message")
                .build();

        RescueMission savedMission = missionRepository.save(mission);

        // Increment active mission counter and update department availability if capacity reached
        chosenDept.setActiveMissionsCount(chosenDept.getActiveMissionsCount() + 1);
        if (chosenDept.getActiveMissionsCount() >= chosenDept.getTotalCapacity()) {
            chosenDept.setIsAvailable(false);
        }
        departmentRepository.save(chosenDept);

        log.info("Successfully created RescueMission ID: {} for Incident ID: {} assigned to Department: {} (ID: {})",
                savedMission.getId(), event.getIncidentId(), chosenDept.getName(), chosenDept.getId());
    }

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentCreateRequest request) {
        RescueDepartment department = RescueDepartment.builder()
                .name(request.getName().trim())
                .type(request.getType())
                .jurisdictionCode(request.getJurisdictionCode().toUpperCase().trim())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .contactPhone(request.getContactPhone())
                .totalCapacity(request.getTotalCapacity() != null ? request.getTotalCapacity() : 10)
                .activeMissionsCount(0)
                .isAvailable(true)
                .build();

        RescueDepartment saved = departmentRepository.save(department);
        log.info("Created Rescue Department: {} (ID: {})", saved.getName(), saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(UUID id, DepartmentUpdateRequest request) {
        RescueDepartment dept = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rescue Department not found with ID: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            dept.setName(request.getName().trim());
        }
        if (request.getType() != null) {
            dept.setType(request.getType());
        }
        if (request.getJurisdictionCode() != null && !request.getJurisdictionCode().isBlank()) {
            dept.setJurisdictionCode(request.getJurisdictionCode().toUpperCase().trim());
        }
        if (request.getLatitude() != null) {
            dept.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            dept.setLongitude(request.getLongitude());
        }
        if (request.getContactPhone() != null && !request.getContactPhone().isBlank()) {
            dept.setContactPhone(request.getContactPhone().trim());
        }
        if (request.getTotalCapacity() != null) {
            dept.setTotalCapacity(request.getTotalCapacity());
        }
        if (request.getIsAvailable() != null) {
            dept.setIsAvailable(request.getIsAvailable());
        }

        RescueDepartment updated = departmentRepository.save(dept);
        log.info("Updated Rescue Department: {} (ID: {})", updated.getName(), updated.getId());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public DepartmentResponse toggleAvailability(UUID id, Boolean isAvailable) {
        RescueDepartment dept = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rescue Department not found with ID: " + id));

        dept.setIsAvailable(isAvailable);
        RescueDepartment updated = departmentRepository.save(dept);
        log.info("Department ID: {} availability set to {}", id, isAvailable);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(UUID id) {
        return departmentRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Rescue Department not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentsByJurisdiction(String jurisdictionCode) {
        return departmentRepository.findByJurisdictionCode(jurisdictionCode.toUpperCase().trim()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> findNearbyDepartments(Double lat, Double lng, Double radiusKm) {
        return departmentRepository.findAvailableWithinRadius(lat, lng, radiusKm).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteDepartment(UUID id) {
        RescueDepartment dept = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rescue Department not found with ID: " + id));

        if (dept.getActiveMissionsCount() > 0) {
            throw new IllegalStateException("Cannot delete a department with active ongoing missions");
        }

        departmentRepository.delete(dept);
        log.info("Deleted Rescue Department ID: {}", id);
    }

    private DepartmentResponse mapToResponse(RescueDepartment d) {
        return DepartmentResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .type(d.getType())
                .jurisdictionCode(d.getJurisdictionCode())
                .latitude(d.getLatitude())
                .longitude(d.getLongitude())
                .contactPhone(d.getContactPhone())
                .totalCapacity(d.getTotalCapacity())
                .activeMissionsCount(d.getActiveMissionsCount())
                .isAvailable(d.getIsAvailable())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    @Slf4j
    @Service
    @RequiredArgsConstructor
    @Transactional(readOnly = true)
    public static class RescueAssignmentServiceImpl implements RescueAssignmentService {

        private final RescueDepartmentRepository departmentRepository;
        private final RescueMissionRepository missionRepository;
        private final RedisGeoService redisGeoService;

        // Search radius in kilometers for auto-assignment
        private static final double SEARCH_RADIUS_KM = 50.0;

        @Override
        @Transactional
        public void autoAssignRescueTeam(IncidentCreatedEvent event) {
            log.info("Starting auto-assignment for Incident ID: [{}] at coordinates ({}, {})",
                    event.getIncidentId(), event.getLatitude(), event.getLongitude());

            // 1. Register incident location in Redis GEO for tracking telemetry
            redisGeoService.registerIncidentLocation(
                    event.getIncidentId(),
                    event.getLongitude(),
                    event.getLatitude()
            );

            // 2. Query available departments with active capacity
            List<RescueDepartment> availableDepartments = departmentRepository.findByIsAvailableTrue();

            if (availableDepartments.isEmpty()) {
                log.warn("Auto-assignment failed for Incident ID: [{}]. No available departments in system.",
                        event.getIncidentId());
                return;
            }

            // 3. Find the spatially closest available department using Haversine calculation
            Optional<RescueDepartment> closestDepartment = availableDepartments.stream()
                    .filter(dept -> dept.getActiveMissionsCount() < dept.getTotalCapacity())
                    .min(Comparator.comparingDouble(dept -> calculateDistanceInKm(
                            event.getLatitude(), event.getLongitude(),
                            dept.getLatitude(), dept.getLongitude()
                    )));

            if (closestDepartment.isEmpty()) {
                log.warn("Auto-assignment failed for Incident ID: [{}]. All available departments are at max capacity.",
                        event.getIncidentId());
                return;
            }

            RescueDepartment assignedDept = closestDepartment.get();
            double distanceKm = calculateDistanceInKm(
                    event.getLatitude(), event.getLongitude(),
                    assignedDept.getLatitude(), assignedDept.getLongitude()
            );

            // 4. Create and persist Rescue Mission
            RescueMission mission = RescueMission.builder()
                    .incidentId(event.getIncidentId())
                    .department(assignedDept)
                    .status(MissionStatus.DISPATCHED)
                    .notes(String.format("Auto-assigned to %s (Dist: %.2f km)", assignedDept.getName(), distanceKm))
                    .build();

            missionRepository.save(mission);

            // 5. Update Department Capacity & Availability
            updateDepartmentCapacity(assignedDept);

            log.info("Successfully assigned Incident ID: [{}] to Department: [{}] (Dist: %.2f km)",
                    event.getIncidentId(), assignedDept.getName(), distanceKm);
        }

        @Override
        @Transactional
        public RescueMission assignDepartmentToIncident(UUID incidentId, UUID departmentId) {
            log.info("Manual assignment requested for Incident ID: [{}] to Department ID: [{}]", incidentId, departmentId);

            RescueDepartment department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new DepartmentNotFoundException("Rescue Department not found with ID: " + departmentId));

            if (department.getActiveMissionsCount() >= department.getTotalCapacity()) {
                throw new IllegalStateException("Department " + department.getName() + " is currently at maximum capacity.");
            }

            RescueMission mission = RescueMission.builder()
                    .incidentId(incidentId)
                    .department(department)
                    .status(MissionStatus.DISPATCHED)
                    .notes("Manually assigned by dispatcher operator.")
                    .build();

            RescueMission savedMission = missionRepository.save(mission);
            updateDepartmentCapacity(department);

            return savedMission;
        }

        // Helper: Updates capacity counts and toggles isAvailable boolean flag
        private void updateDepartmentCapacity(RescueDepartment department) {
            department.setActiveMissionsCount(department.getActiveMissionsCount() + 1);

            if (department.getActiveMissionsCount() >= department.getTotalCapacity()) {
                department.setIsAvailable(false);
                log.info("Department [{}] reached maximum capacity. Marked unavailable.", department.getName());
            }

            departmentRepository.save(department);
        }

        // Helper: Haversine formula for distance calculation between coordinates in KM
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
}