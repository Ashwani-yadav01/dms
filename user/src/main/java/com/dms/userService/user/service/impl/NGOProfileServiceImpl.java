package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.NGOProfileRequest;
import com.dms.userService.user.dto.response.NGOProfileResponse;
import com.dms.userService.user.service.NGOProfileService;

import java.util.UUID;

public class NGOProfileServiceImpl implements NGOProfileService {
    @Override
    public NGOProfileResponse createNGO(NGOProfileRequest request) {
        return null;
    }

    @Override
    public NGOProfileResponse getNGO(UUID userId) {
        return null;
    }

    @Override
    public NGOProfileResponse updateNGO(UUID userId, NGOProfileRequest request) {
        return null;
    }

    @Override
    public void deleteNGO(UUID userId) {

    }

    @Override
    public NGOProfileResponse getNGOByRegistrationNumber(String registrationNumber) {
        return null;
    }
}
