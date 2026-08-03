package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.GovernmentOfficialProfileRequest;
import com.dms.userService.user.dto.response.GovernmentOfficialProfileResponse;
import com.dms.userService.user.service.GovernmentOfficialProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class GovernmentOfficialProfileServiceImpl implements GovernmentOfficialProfileService {

    @Override
    public boolean existsById(UUID userId) {
        return false;
    }

    @Override
    public GovernmentOfficialProfileResponse createProfile(UUID userId, GovernmentOfficialProfileRequest request) {
        return null;
    }

    @Override
    public GovernmentOfficialProfileResponse getProfile(UUID userId) {
        return null;
    }

    @Override
    public GovernmentOfficialProfileResponse updateProfile(UUID userId, GovernmentOfficialProfileRequest request) {
        return null;
    }

    @Override
    public void deleteProfile(UUID userId) {

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
