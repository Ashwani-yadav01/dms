package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.VolunteerProfileRequest;
import com.dms.userService.user.dto.response.VolunteerProfileResponse;
import com.dms.userService.user.service.VolunteerProfileService;

import java.util.List;
import java.util.UUID;

public class VolunteerProfileServiceImpl implements VolunteerProfileService {
    @Override
    public VolunteerProfileResponse createVolunteer(VolunteerProfileRequest request) {
        return null;
    }

    @Override
    public VolunteerProfileResponse getVolunteer(UUID userId) {
        return null;
    }

    @Override
    public VolunteerProfileResponse updateVolunteer(UUID userId, VolunteerProfileRequest request) {
        return null;
    }

    @Override
    public void deleteVolunteer(UUID userId) {

    }

    @Override
    public List<VolunteerProfileResponse> getVolunteersByAvailability(String availability) {
        return List.of();
    }

    @Override
    public List<VolunteerProfileResponse> getVolunteersBySkill(String skill) {
        return List.of();
    }
}
