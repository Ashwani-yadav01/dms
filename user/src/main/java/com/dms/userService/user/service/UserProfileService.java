package com.dms.userService.user.service;

import com.dms.userService.user.dto.request.UserProfileRequest;
import com.dms.userService.user.dto.response.UserProfileResponse;
import com.dms.userService.user.entity.UserProfile;

import java.util.UUID;

public interface UserProfileService {
    UserProfileResponse createProfile(UserProfileRequest request);

    UserProfileResponse getProfile(UUID userId);

    UserProfileResponse updateProfile(UUID userId,
                                         UserProfileRequest request);

    UserProfileResponse updateProfilePhoto(UUID userId,
                                           String profilePhotoUrl);

    void deleteProfile(UUID userId);
}
