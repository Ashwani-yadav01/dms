package com.dms.userService.user.service;

import com.dms.userService.user.dto.request.VolunteerProfileRequest;
import com.dms.userService.user.dto.response.VolunteerProfileResponse;

import java.util.List;
import java.util.UUID;

public interface VolunteerProfileService {
    VolunteerProfileResponse createVolunteer(VolunteerProfileRequest request);

    VolunteerProfileResponse getVolunteer(UUID userId);

    VolunteerProfileResponse updateVolunteer(UUID userId,
                                             VolunteerProfileRequest request);

    void deleteVolunteer(UUID userId);

    List<VolunteerProfileResponse> getVolunteersByAvailability(String availability);

    List<VolunteerProfileResponse> getVolunteersBySkill(String skill);
}
