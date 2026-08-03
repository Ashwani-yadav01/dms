package com.dms.userService.user.service;

import com.dms.userService.user.dto.request.UserProfileRequest;
import com.dms.userService.user.dto.response.UserProfileResponse;

import java.util.UUID;

public interface UserProfileFacadeService {

    UserProfileResponse createProfile(UUID userId, UserProfileRequest request);

    UserProfileResponse getProfile(UUID userId);

    UserProfileResponse updateProfile(UUID userId, UserProfileRequest request);

    UserProfileResponse updatePhoto(UUID userId, String profilePhotoUrl);

    void deleteProfile(UUID userId);
}