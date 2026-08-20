package com.dms.rescueService.rescue.service;

import com.dms.common.events.IncidentCreatedEvent;
import com.dms.rescueService.rescue.dto.request.DepartmentCreateRequest;
import com.dms.rescueService.rescue.dto.request.DepartmentUpdateRequest;
import com.dms.rescueService.rescue.dto.response.DepartmentResponse;

import java.util.List;
import java.util.UUID;

public interface DepartmentService {
//    void processAutoDispatchForIncident(IncidentCreatedEvent event);
    DepartmentResponse createDepartment(DepartmentCreateRequest request);
    DepartmentResponse updateDepartment(UUID id, DepartmentUpdateRequest request);
    DepartmentResponse toggleAvailability(UUID id, Boolean isAvailable);
    DepartmentResponse getDepartmentById(UUID id);
    List<DepartmentResponse> getAllDepartments();
    List<DepartmentResponse> getDepartmentsByJurisdiction(String jurisdictionCode);
    List<DepartmentResponse> findNearbyDepartments(Double lat, Double lng, Double radiusKm);
    void deleteDepartment(UUID id);
}