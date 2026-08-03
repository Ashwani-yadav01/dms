package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.CitizenProfileRequest;
import com.dms.userService.user.dto.response.CitizenProfileResponse;
import com.dms.userService.user.service.CitizenProfileService;

import java.util.UUID;

public class CitizenProfileServiceImpl implements CitizenProfileService {
    @Override
    public boolean existsById(UUID userId) {
        return false;
    }

    @Override
    public CitizenProfileResponse createProfile(UUID userId, CitizenProfileRequest request) {
        return null;
    }

    @Override
    public CitizenProfileResponse getProfile(UUID userId) {
        return null;
    }

    @Override
    public CitizenProfileResponse updateProfile(UUID userId, CitizenProfileRequest request) {
        return null;
    }

    @Override
    public void deleteProfile(UUID userId) {

    }
}
