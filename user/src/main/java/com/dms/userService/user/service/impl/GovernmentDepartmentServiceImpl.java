package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.GovernmentDepartmentRequest;
import com.dms.userService.user.dto.response.GovernmentDepartmentResponse;
import com.dms.userService.user.service.GovernmentDepartmentService;

import java.util.List;
import java.util.UUID;

public class GovernmentDepartmentServiceImpl implements GovernmentDepartmentService {
    @Override
    public GovernmentDepartmentResponse createDepartment(GovernmentDepartmentRequest request) {
        return null;
    }

    @Override
    public GovernmentDepartmentResponse getDepartment(UUID departmentId) {
        return null;
    }

    @Override
    public List<GovernmentDepartmentResponse> getAllDepartments() {
        return List.of();
    }

    @Override
    public GovernmentDepartmentResponse updateDepartment(UUID departmentId, GovernmentDepartmentRequest request) {
        return null;
    }

    @Override
    public void deleteDepartment(UUID departmentId) {

    }
}
