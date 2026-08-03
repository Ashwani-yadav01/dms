package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.CitizenProfileRequest;
import com.dms.userService.user.dto.response.CitizenProfileResponse;
import com.dms.userService.user.entity.CitizenProfile;
import com.dms.userService.user.entity.Gender;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.exception.UserAlreadyExistsException;
import com.dms.userService.user.exception.UserNotFoundException;
import com.dms.userService.user.repository.CitizenProfileRepository;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.service.CitizenProfileService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor//completed
public class CitizenProfileServiceImpl implements CitizenProfileService {

    private final CitizenProfileRepository citizenProfileRepository;
    private final UserRepository userRepository;
    private final ModelMapper mapper;

    @Override
    public boolean existsById(UUID userId) {
        return citizenProfileRepository.existsById(userId);
    }

    @Override
    @Transactional
    public CitizenProfileResponse createProfile(UUID userId, CitizenProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (citizenProfileRepository.existsById(userId)) {
            throw new UserAlreadyExistsException("Profile for user with id: " + userId + " already exists");
        }

        CitizenProfile citizenProfile = mapper.map(request, CitizenProfile.class);

        citizenProfile.setUser(user);

        CitizenProfile savedProfile = citizenProfileRepository.save(citizenProfile);
        return mapper.map(savedProfile, CitizenProfileResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public CitizenProfileResponse getProfile(UUID userId) {
        CitizenProfile citizenProfile = citizenProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Profile not found for user id: " + userId));

        return mapper.map(citizenProfile, CitizenProfileResponse.class);
    }

    @Override
    @Transactional
    public CitizenProfileResponse updateProfile(UUID userId, CitizenProfileRequest request) {
        CitizenProfile citizenProfile = citizenProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Profile not found for user id: " + userId));

        citizenProfile.setName(request.getName());
        citizenProfile.setAddressLine(request.getAddressLine());
        citizenProfile.setCity(request.getCity());
        citizenProfile.setState(request.getState());
        citizenProfile.setDistrict(request.getDistrict());
        citizenProfile.setPincode(request.getPincode());
        citizenProfile.setLatitude(request.getLatitude());
        citizenProfile.setLongitude(request.getLongitude());
        if (request.getProfilePhotoUrl() != null) {
            citizenProfile.setProfilePhotoUrl(request.getProfilePhotoUrl());
        }

        if (request.getGender() != null) {
            citizenProfile.setGender(Gender.valueOf(request.getGender().toUpperCase()));
        }
        citizenProfile.setDateOfBirth(request.getDateOfBirth());
        citizenProfile.setOccupation(request.getOccupation());

        CitizenProfile updatedProfile = citizenProfileRepository.save(citizenProfile);

        return mapper.map(updatedProfile, CitizenProfileResponse.class);
    }

    @Override
    @Transactional
    public void deleteProfile(UUID userId) {
        if (!citizenProfileRepository.existsById(userId)) {
            throw new UserNotFoundException("Profile not found for user id: " + userId);
        }
        citizenProfileRepository.deleteById(userId);
    }
}