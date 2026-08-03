package com.dms.userService.user.service;

import com.dms.userService.user.dto.request.GovernmentOfficialProfileRequest;
import com.dms.userService.user.dto.response.GovernmentOfficialProfileResponse;
import com.dms.userService.user.entity.DepartmentCategory;
import com.dms.userService.user.entity.HierarchyLevel;
import com.dms.userService.user.entity.OfficialStatus;

import java.util.List;
import java.util.UUID;

public interface GovernmentOfficialProfileService {

    boolean existsById(UUID userId);

    GovernmentOfficialProfileResponse createProfile(UUID userId, GovernmentOfficialProfileRequest request);

    GovernmentOfficialProfileResponse getProfile(UUID userId);

    GovernmentOfficialProfileResponse updateProfile(UUID userId, GovernmentOfficialProfileRequest request);

    void deleteProfile(UUID userId);
}