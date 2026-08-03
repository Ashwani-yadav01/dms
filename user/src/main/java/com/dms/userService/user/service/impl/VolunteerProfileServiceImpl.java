package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.VolunteerProfileRequest;
import com.dms.userService.user.dto.response.VolunteerProfileResponse;
import com.dms.userService.user.entity.Availability;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.entity.VolunteerProfile;
import com.dms.userService.user.exception.UserAlreadyExistsException;
import com.dms.userService.user.exception.UserNotFoundException;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.repository.VolunteerProfileRepository;
import com.dms.userService.user.service.VolunteerProfileService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor//completed
public class VolunteerProfileServiceImpl implements VolunteerProfileService {

    private final VolunteerProfileRepository volunteerProfileRepository;
    private final UserRepository userRepository;
    private final ModelMapper mapper;

    @Override
    public boolean existsById(UUID userId) {
        return volunteerProfileRepository.existsById(userId);
    }

    @Override
    @Transactional
    public VolunteerProfileResponse createProfile(UUID userId, VolunteerProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (volunteerProfileRepository.existsById(userId)) {
            throw new UserAlreadyExistsException("Profile for user with id: " + userId + " already exists");
        }

        VolunteerProfile volunteerProfile = mapper.map(request, VolunteerProfile.class);
        volunteerProfile.setId(userId);
        volunteerProfile.setUser(user);

        VolunteerProfile savedProfile = volunteerProfileRepository.save(volunteerProfile);
        return mapper.map(savedProfile, VolunteerProfileResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public VolunteerProfileResponse getProfile(UUID userId) {
        VolunteerProfile volunteerProfile = volunteerProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Profile not found for user id: " + userId));

        return mapper.map(volunteerProfile, VolunteerProfileResponse.class);
    }

    @Override
    @Transactional
    public VolunteerProfileResponse updateProfile(UUID userId, VolunteerProfileRequest request) {
        VolunteerProfile volunteerProfile = volunteerProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Profile not found for user id: " + userId));

        volunteerProfile.setName(request.getName());
        volunteerProfile.setAddressLine(request.getAddressLine());
        volunteerProfile.setCity(request.getCity());
        volunteerProfile.setState(request.getState());
        volunteerProfile.setDistrict(request.getDistrict());
        volunteerProfile.setPincode(request.getPincode());
        volunteerProfile.setLatitude(request.getLatitude());
        volunteerProfile.setLongitude(request.getLongitude());
        if (request.getProfilePhotoUrl() != null) {
            volunteerProfile.setProfilePhotoUrl(request.getProfilePhotoUrl());
        }

        if (request.getSkills() != null) {
            volunteerProfile.getSkills().clear();
            volunteerProfile.getSkills().addAll(request.getSkills());
        }

        volunteerProfile.setExperienceInYears(request.getExperienceInYears());

        if (request.getAvailability() != null) {
            volunteerProfile.setAvailability(Availability.valueOf(request.getAvailability().toUpperCase()));
        }

        VolunteerProfile updatedProfile = volunteerProfileRepository.save(volunteerProfile);
        return mapper.map(updatedProfile, VolunteerProfileResponse.class);
    }
    @Override
    @Transactional
    public void deleteProfile(UUID userId) {
        if (!volunteerProfileRepository.existsById(userId)) {
            throw new UserNotFoundException("Profile not found for user id: " + userId);
        }
        volunteerProfileRepository.deleteById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolunteerProfileResponse> getVolunteersByAvailability(String availability) {
        Availability availabilityEnum = Availability.valueOf(availability.toUpperCase());

        return volunteerProfileRepository.findByAvailability(availabilityEnum)
                .stream()
                .map(profile -> mapper.map(profile, VolunteerProfileResponse.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolunteerProfileResponse> getVolunteersBySkill(String skill) {
        return volunteerProfileRepository.findBySkill(skill)
                .stream()
                .map(profile -> mapper.map(profile, VolunteerProfileResponse.class))
                .toList();
    }
}