package com.dms.userService.user.service;

//import com.dms.userService.user.dto.request.IncidentAllocationRequest;
import com.dms.userService.user.dto.response.GovernmentOfficialProfileResponse;
//import com.dms.userService.user.dto.response.OfficialAllocationResult;
import com.dms.userService.user.entity.DepartmentCategory;
import com.dms.userService.user.entity.HierarchyLevel;
import com.dms.userService.user.entity.OfficialStatus;

import java.util.List;
import java.util.UUID;

public interface GovernmentOfficialDomainService {

    // --- DOMAIN SEARCH & HIERARCHY ---
    GovernmentOfficialProfileResponse getOfficialByEmployeeId(String employeeId);

    List<GovernmentOfficialProfileResponse> getOfficialsByDepartmentName(String departmentName);

    List<GovernmentOfficialProfileResponse> getOfficialsByHierarchy(HierarchyLevel hierarchyLevel);

    GovernmentOfficialProfileResponse getSupervisor(UUID userId);

    List<GovernmentOfficialProfileResponse> getSubordinates(UUID userId);

    List<String> getDepartmentSuggestions();

    // --- REAL-TIME STATUS & VERIFICATION ---
    GovernmentOfficialProfileResponse updateOfficialStatus(UUID userId, OfficialStatus status);

    GovernmentOfficialProfileResponse verifyOfficial(UUID userId, boolean isVerified);

    // --- SITUATIONAL ALLOCATION ENGINE ---
    List<GovernmentOfficialProfileResponse> findEligibleOfficials(
            OfficialStatus status,
            DepartmentCategory category,
            HierarchyLevel level
    );

//    OfficialAllocationResult alocateClosestOfficial(IncidentAllocationRequest request);

//    List<OfficialAllocationResult> allocateMultipleClosestOfficials(IncidentAllocationRequest request, int limit);
}//comment parts will be implemented when incident service is created