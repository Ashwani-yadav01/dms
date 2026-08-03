package com.dms.userService.user.service;

import com.dms.userService.user.dto.request.NGOProfileRequest;
import com.dms.userService.user.dto.response.NGOProfileResponse;

import java.util.UUID;

public interface NGOProfileService {

    boolean existsById(UUID userId);

    NGOProfileResponse createProfile(UUID userId, NGOProfileRequest request);

    NGOProfileResponse getProfile(UUID userId);

    NGOProfileResponse updateProfile(UUID userId, NGOProfileRequest request);

    void deleteProfile(UUID userId);

    // Specific domain queries
    NGOProfileResponse getNGOByRegistrationNumber(String registrationNumber);
}