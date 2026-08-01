package com.dms.userService.user.service;

import com.dms.userService.user.dto.request.GovernmentDepartmentRequest;
import com.dms.userService.user.dto.response.GovernmentDepartmentResponse;

import java.util.List;
import java.util.UUID;

public interface GovernmentDepartmentService {

    GovernmentDepartmentResponse createDepartment(
            GovernmentDepartmentRequest request);

    GovernmentDepartmentResponse getDepartment(UUID departmentId);

    List<GovernmentDepartmentResponse> getAllDepartments();

    GovernmentDepartmentResponse updateDepartment(
            UUID departmentId,
            GovernmentDepartmentRequest request);

    void deleteDepartment(UUID departmentId);
}