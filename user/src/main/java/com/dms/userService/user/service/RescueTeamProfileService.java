package com.dms.userService.user.service;

import com.dms.userService.user.dto.request.RescueTeamProfileRequest;
import com.dms.userService.user.dto.response.RescueTeamProfileResponse;
import java.util.UUID;

public interface RescueTeamProfileService {
    boolean existsById(UUID userId);
    RescueTeamProfileResponse createProfile(UUID userId, RescueTeamProfileRequest request);
    RescueTeamProfileResponse getProfile(UUID userId);
    RescueTeamProfileResponse updateProfile(UUID userId, RescueTeamProfileRequest request);
    RescueTeamProfileResponse getProfileForAdmin(UUID userId, UUID actorId);
    RescueTeamProfileResponse verifyProfile(UUID userId, UUID actorId);
    void deleteProfile(UUID userId);
}
