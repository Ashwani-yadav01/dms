package com.dms.userService.user.service;

import com.dms.userService.user.dto.request.CitizenProfileRequest;
import com.dms.userService.user.dto.response.CitizenProfileResponse;

import java.util.UUID;

public interface CitizenProfileService {

    boolean existsById(UUID userId);

    CitizenProfileResponse createProfile(UUID userId, CitizenProfileRequest request);

    CitizenProfileResponse getProfile(UUID userId);

    CitizenProfileResponse updateProfile(UUID userId, CitizenProfileRequest request);

    void deleteProfile(UUID userId);
}