package com.dms.userService.user.service;

import com.dms.userService.user.dto.request.NGOProfileRequest;
import com.dms.userService.user.dto.response.NGOProfileResponse;

import java.util.UUID;

public interface NGOProfileService {
    NGOProfileResponse createNGO(
            NGOProfileRequest request);

    NGOProfileResponse getNGO(UUID userId);

    NGOProfileResponse updateNGO(
            UUID userId,
            NGOProfileRequest request);

    void deleteNGO(UUID userId);

    NGOProfileResponse getNGOByRegistrationNumber(
            String registrationNumber);
}
