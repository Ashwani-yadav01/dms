package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.UserProfileRequest;
import com.dms.userService.user.dto.response.UserProfileResponse;
import com.dms.userService.user.service.UserProfileService;

import java.util.UUID;

public class UserProfileServiceImpl implements UserProfileService {
    @Override
    public UserProfileResponse createProfile(UserProfileRequest request) {
        return null;
    }

    @Override
    public UserProfileResponse getProfile(UUID userId) {
        return null;
    }

    @Override
    public UserProfileResponse updateProfile(UUID userId, UserProfileRequest request) {
        return null;
    }

    @Override
    public UserProfileResponse updateProfilePhoto(UUID userId, String profilePhotoUrl) {
        return null;
    }

    @Override
    public void deleteProfile(UUID userId) {

    }
}
