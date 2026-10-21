package com.dms.userService.user.service;

import com.dms.userService.user.dto.request.DistrictAdminProfileRequest;
import com.dms.userService.user.dto.response.DistrictAdminProfileResponse;

import java.util.UUID;

public interface DistrictAdminProfileService {

    boolean existsById(UUID userId);

    DistrictAdminProfileResponse createProfile(UUID userId, DistrictAdminProfileRequest request);

    DistrictAdminProfileResponse getProfile(UUID userId);

    DistrictAdminProfileResponse updateProfile(UUID userId, DistrictAdminProfileRequest request);

    void deleteProfile(UUID userId);
}
