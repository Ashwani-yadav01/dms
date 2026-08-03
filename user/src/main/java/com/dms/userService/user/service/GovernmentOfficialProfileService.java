package com.dms.userService.user.service;

import com.dms.userService.user.dto.request.GovernmentOfficialProfileRequest;
import com.dms.userService.user.dto.response.GovernmentOfficialProfileResponse;

import java.util.List;
import java.util.UUID;

public interface GovernmentOfficialProfileService {

    boolean existsById(UUID userId);

    GovernmentOfficialProfileResponse createProfile(UUID userId, GovernmentOfficialProfileRequest request);

    GovernmentOfficialProfileResponse getProfile(UUID userId);

    GovernmentOfficialProfileResponse updateProfile(UUID userId, GovernmentOfficialProfileRequest request);

    void deleteProfile(UUID userId);

    // Specific domain queries
    GovernmentOfficialProfileResponse getOfficialByEmployeeId(String employeeId);

    List<GovernmentOfficialProfileResponse> getOfficialByDepartment(UUID departmentId);

    List<GovernmentOfficialProfileResponse> getOfficialsByHierarchy(Integer hierarchyLevel);

    GovernmentOfficialProfileResponse getSupervisor(UUID userId);

    List<GovernmentOfficialProfileResponse> getSubordinates(UUID userId);
}