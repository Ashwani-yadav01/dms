package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.NGOProfileRequest;
import com.dms.userService.user.dto.response.NGOProfileResponse;
import com.dms.userService.user.entity.NGOProfile;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.exception.ResourceNotFoundException;
import com.dms.userService.user.exception.UserAlreadyExistsException;
import com.dms.userService.user.exception.UserNotFoundException;
import com.dms.userService.user.repository.NGOProfileRepository;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.service.NGOProfileService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor//completed
public class NGOProfileServiceImpl implements NGOProfileService {

    private final NGOProfileRepository ngoProfileRepository;
    private final UserRepository userRepository;
    private final ModelMapper mapper;

    @Override
    public boolean existsById(UUID userId) {
        return ngoProfileRepository.existsById(userId);
    }

    @Override
    @Transactional
    public NGOProfileResponse createProfile(UUID userId, NGOProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (ngoProfileRepository.existsById(userId)) {
            throw new UserAlreadyExistsException("Profile for user with id: " + userId + " already exists");
        }

        if (ngoProfileRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new UserAlreadyExistsException("NGO with registration number " + request.getRegistrationNumber() + " already exists");
        }

        NGOProfile ngoProfile = mapper.map(request, NGOProfile.class);
        ngoProfile.setId(userId);
        ngoProfile.setUser(user);

        NGOProfile savedProfile = ngoProfileRepository.save(ngoProfile);
        return mapper.map(savedProfile, NGOProfileResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public NGOProfileResponse getProfile(UUID userId) {
        NGOProfile ngoProfile = ngoProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Profile not found for user id: " + userId));

        return mapper.map(ngoProfile, NGOProfileResponse.class);
    }

    @Override
    @Transactional
    public NGOProfileResponse updateProfile(UUID userId, NGOProfileRequest request) {
        NGOProfile ngoProfile = ngoProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Profile not found for user id: " + userId));

        ngoProfile.setName(request.getName());
        ngoProfile.setAddressLine(request.getAddressLine());
        ngoProfile.setCity(request.getCity());
        ngoProfile.setState(request.getState());
        ngoProfile.setDistrict(request.getDistrict());
        ngoProfile.setPincode(request.getPincode());
        ngoProfile.setLatitude(request.getLatitude());
        ngoProfile.setLongitude(request.getLongitude());
        if (request.getProfilePhotoUrl() != null) {
            ngoProfile.setProfilePhotoUrl(request.getProfilePhotoUrl());
        }

        ngoProfile.setNgoName(request.getNgoName());
        ngoProfile.setRegistrationNumber(request.getRegistrationNumber());
        ngoProfile.setOwnerName(request.getOwnerName());
        ngoProfile.setWebsite(request.getWebsite());
        ngoProfile.setDescription(request.getDescription());

        NGOProfile updatedProfile = ngoProfileRepository.save(ngoProfile);
        return mapper.map(updatedProfile, NGOProfileResponse.class);
    }

    @Override
    @Transactional
    public void deleteProfile(UUID userId) {
        if (!ngoProfileRepository.existsById(userId)) {
            throw new UserNotFoundException("Profile not found for user id: " + userId);
        }
        ngoProfileRepository.deleteById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public NGOProfileResponse getNGOByRegistrationNumber(String registrationNumber) {
        NGOProfile ngoProfile = ngoProfileRepository.findByRegistrationNumber(registrationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("NGO does not exist with registration number: " + registrationNumber));

        return mapper.map(ngoProfile, NGOProfileResponse.class);
    }
}