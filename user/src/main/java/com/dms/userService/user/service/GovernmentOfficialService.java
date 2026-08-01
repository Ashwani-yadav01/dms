package com.dms.userService.user.service;

import com.dms.userService.user.dto.request.GovernmentOfficialProfileRequest;
import com.dms.userService.user.dto.response.GovernmentOfficialProfileResponse;

import java.util.List;
import java.util.UUID;

public interface GovernmentOfficialService {

    GovernmentOfficialProfileResponse createOfficial(GovernmentOfficialProfileRequest request);

    GovernmentOfficialProfileResponse getOfficial(UUID userId);

    GovernmentOfficialProfileResponse updateOfficial(UUID userId, GovernmentOfficialProfileRequest request);

    void deleteOfficial(UUID userId);

    GovernmentOfficialProfileResponse getOfficialByEmployeeId(String employeeId);

    List<GovernmentOfficialProfileResponse> getOfficialByDepartment(UUID departmentId);

    List<GovernmentOfficialProfileResponse> getOfficialsByHierarchy(Integer hierarchyLevel);

    GovernmentOfficialProfileResponse getSupervisor(UUID userId);

    List<GovernmentOfficialProfileResponse> getSubordinates(UUID userId);
}