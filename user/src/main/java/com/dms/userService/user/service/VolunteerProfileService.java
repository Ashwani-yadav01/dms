package com.dms.userService.user.service;

import com.dms.userService.user.dto.request.VolunteerProfileRequest;
import com.dms.userService.user.dto.response.VolunteerProfileResponse;

import java.util.List;
import java.util.UUID;

public interface VolunteerProfileService {

    boolean existsById(UUID userId);

    VolunteerProfileResponse createProfile(UUID userId, VolunteerProfileRequest request);

    VolunteerProfileResponse getProfile(UUID userId);

    VolunteerProfileResponse updateProfile(UUID userId, VolunteerProfileRequest request);

    void deleteProfile(UUID userId);

    // Specific domain queries
    List<VolunteerProfileResponse> getVolunteersByAvailability(String availability);

    List<VolunteerProfileResponse> getVolunteersBySkill(String skill);
}