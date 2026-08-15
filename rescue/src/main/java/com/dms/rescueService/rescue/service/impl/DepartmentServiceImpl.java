package com.dms.rescueService.rescue.service.impl;

import com.dms.common.events.IncidentCreatedEvent;
import com.dms.rescueService.rescue.dto.request.DepartmentCreateRequest;
import com.dms.rescueService.rescue.dto.request.DepartmentUpdateRequest;
import com.dms.rescueService.rescue.dto.response.DepartmentResponse;
import com.dms.rescueService.rescue.entity.MissionStatus;
import com.dms.rescueService.rescue.entity.RescueDepartment;
import com.dms.rescueService.rescue.entity.RescueMission;
import com.dms.rescueService.rescue.repository.RescueDepartmentRepository;
import com.dms.rescueService.rescue.repository.RescueMissionRepository;
import com.dms.rescueService.rescue.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
}