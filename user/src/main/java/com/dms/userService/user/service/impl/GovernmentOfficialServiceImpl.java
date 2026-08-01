package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.GovernmentOfficialProfileRequest;
import com.dms.userService.user.dto.response.GovernmentOfficialProfileResponse;
import com.dms.userService.user.service.GovernmentOfficialService;

import java.util.List;
import java.util.UUID;

public class GovernmentOfficialServiceImpl implements GovernmentOfficialService {
    @Override
    public GovernmentOfficialProfileResponse createOfficial(GovernmentOfficialProfileRequest request) {
        return null;
    }

    @Override
    public GovernmentOfficialProfileResponse getOfficial(UUID userId) {
        return null;
    }

    @Override
    public GovernmentOfficialProfileResponse updateOfficial(UUID userId, GovernmentOfficialProfileRequest request) {
        return null;
    }

    @Override
    public void deleteOfficial(UUID userId) {

    }

    @Override
    public GovernmentOfficialProfileResponse getOfficialByEmployeeId(String employeeId) {
        return null;
    }

    @Override
    public List<GovernmentOfficialProfileResponse> getOfficialByDepartment(UUID departmentId) {
        return List.of();
    }

    @Override
    public List<GovernmentOfficialProfileResponse> getOfficialsByHierarchy(Integer hierarchyLevel) {
        return List.of();
    }

    @Override
    public GovernmentOfficialProfileResponse getSupervisor(UUID userId) {
        return null;
    }

    @Override
    public List<GovernmentOfficialProfileResponse> getSubordinates(UUID userId) {
        return List.of();
    }
}
