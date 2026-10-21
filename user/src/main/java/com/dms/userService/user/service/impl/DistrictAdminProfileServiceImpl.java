package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.DistrictAdminProfileRequest;
import com.dms.userService.user.dto.response.DistrictAdminProfileResponse;
import com.dms.userService.user.entity.DistrictAdminProfile;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.exception.UserAlreadyExistsException;
import com.dms.userService.user.exception.UserNotFoundException;
import com.dms.userService.user.repository.DistrictAdminProfileRepository;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.service.DistrictAdminProfileService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DistrictAdminProfileServiceImpl implements DistrictAdminProfileService {

    private final DistrictAdminProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final com.dms.userService.user.repository.DistrictAdminRegistryRepository districtAdminRegistryRepository;
    private final ModelMapper mapper;

    @Override
    public boolean existsById(UUID userId) {
        return profileRepository.existsById(userId);
    }

    @Override
    @Transactional
    public DistrictAdminProfileResponse createProfile(UUID userId, DistrictAdminProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (profileRepository.existsById(userId)) {
            throw new UserAlreadyExistsException("Profile for user with id: " + userId + " already exists");
        }

        DistrictAdminProfile profile = mapper.map(request, DistrictAdminProfile.class);
        profile.setUser(user);

        if (request.getHelplineNumber() != null) {
            profile.setHelplineNumber(request.getHelplineNumber());
            profile.setControlRoomHelpline(request.getHelplineNumber());
        }

        // Pre-assigned category and registry credentials from Super Admin invitation take precedence
        if (user.getEmail() != null) {
            districtAdminRegistryRepository.findByAdminEmailIgnoreCase(user.getEmail())
                    .ifPresent(registry -> {
                        if (registry.getDepartmentCategory() != null && !registry.getDepartmentCategory().isBlank()) {
                            try {
                                profile.setDepartmentCategory(com.dms.userService.user.entity.DepartmentCategory.valueOf(registry.getDepartmentCategory().toUpperCase()));
                            } catch (Exception ignored) {}
                        }
                        if (profile.getDistrict() == null && registry.getDistrictName() != null) {
                            profile.setDistrict(registry.getDistrictName());
                        }
                        if (profile.getDistrictName() == null && registry.getDistrictName() != null) {
                            profile.setDistrictName(registry.getDistrictName());
                        }
                        if (profile.getEmployeeId() == null && registry.getEmployeeId() != null) {
                            profile.setEmployeeId(registry.getEmployeeId());
                        }
                        if (profile.getVerificationCode() == null && registry.getAuthorizationCode() != null) {
                            profile.setVerificationCode(registry.getAuthorizationCode());
                        }
                    });
        }

        DistrictAdminProfile savedProfile = profileRepository.save(profile);
        user.setProfileCompleted(true);
        userRepository.save(user);

        DistrictAdminProfileResponse response = mapper.map(savedProfile, DistrictAdminProfileResponse.class);
        response.setHelplineNumber(savedProfile.getHelplineNumber());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public DistrictAdminProfileResponse getProfile(UUID userId) {
        DistrictAdminProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("District admin profile not found for user id: " + userId));

        DistrictAdminProfileResponse response = mapper.map(profile, DistrictAdminProfileResponse.class);
        response.setHelplineNumber(profile.getHelplineNumber());
        return response;
    }

    @Override
    @Transactional
    public DistrictAdminProfileResponse updateProfile(UUID userId, DistrictAdminProfileRequest request) {
        DistrictAdminProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("District admin profile not found for user id: " + userId));

        profile.setName(request.getName());
        profile.setAddressLine(request.getAddressLine());
        profile.setCity(request.getCity());
        profile.setState(request.getState());
        profile.setDistrict(request.getDistrict());
        profile.setPincode(request.getPincode());
        profile.setLatitude(request.getLatitude());
        profile.setLongitude(request.getLongitude());
        if (request.getProfilePhotoUrl() != null) {
            profile.setProfilePhotoUrl(request.getProfilePhotoUrl());
        }

        if (request.getEmployeeId() != null && !request.getEmployeeId().isBlank()) {
            profile.setEmployeeId(request.getEmployeeId());
        }
        if (request.getVerificationCode() != null && !request.getVerificationCode().isBlank()) {
            profile.setVerificationCode(request.getVerificationCode());
        }
        String helpline = request.getHelplineNumber();
        if (helpline != null) {
            profile.setHelplineNumber(helpline);
            profile.setControlRoomHelpline(helpline);
        }

        if (request.getDistrictName() != null) {
            profile.setDistrictName(request.getDistrictName());
        } else if (request.getDistrict() != null) {
            profile.setDistrictName(request.getDistrict());
        }
        if (request.getJurisdiction() != null) profile.setJurisdiction(request.getJurisdiction());
        if (request.getDesignation() != null) profile.setDesignation(request.getDesignation());
        if (request.getDepartment() != null) profile.setDepartment(request.getDepartment());
        if (request.getDepartmentCategory() != null) profile.setDepartmentCategory(request.getDepartmentCategory());

        DistrictAdminProfile updated = profileRepository.save(profile);
        DistrictAdminProfileResponse response = mapper.map(updated, DistrictAdminProfileResponse.class);
        response.setHelplineNumber(updated.getHelplineNumber());
        return response;
    }

    @Override
    @Transactional
    public void deleteProfile(UUID userId) {
        if (!profileRepository.existsById(userId)) {
            throw new UserNotFoundException("District admin profile not found for user id: " + userId);
        }
        profileRepository.deleteById(userId);
    }
}
