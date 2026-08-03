package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.GovernmentOfficialProfileRequest;
import com.dms.userService.user.dto.response.GovernmentOfficialProfileResponse;
import com.dms.userService.user.entity.GovernmentOfficialProfile;
import com.dms.userService.user.entity.OfficialStatus;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.exception.ResourceNotFoundException;
import com.dms.userService.user.exception.UserAlreadyExistsException;
import com.dms.userService.user.exception.UserNotFoundException;
import com.dms.userService.user.repository.GovernmentOfficialProfileRepository;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.service.GovernmentOfficialProfileService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GovernmentOfficialProfileServiceImpl implements GovernmentOfficialProfileService {

    private final GovernmentOfficialProfileRepository officialProfileRepository;
    private final UserRepository userRepository;
    private final ModelMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID userId) {
        return officialProfileRepository.existsById(userId);
    }

    @Override
    @Transactional
    public GovernmentOfficialProfileResponse createProfile(UUID userId, GovernmentOfficialProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (officialProfileRepository.existsById(userId)) {
            throw new UserAlreadyExistsException("Profile already exists for user id: " + userId);
        }

        if (officialProfileRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new UserAlreadyExistsException("Official with employee ID " + request.getEmployeeId() + " already exists");
        }

        // 1. Instantiate and map fields explicitly to prevent ModelMapper converter bugs
        GovernmentOfficialProfile profile = mapToEntity(request);

        // 2. Set JPA relationship (@MapsId handles ID propagation)
        profile.setUser(user);
        profile.setStatus(OfficialStatus.AVAILABLE);
        profile.setIsVerified(false);

        // 3. Update User profile completion status
        user.setProfileCompleted(true);
        user.setUserProfile(profile);

        // 4. Safely resolve and set supervisor if provided
        if (request.getReportsToUserId() != null) {
            GovernmentOfficialProfile supervisor = officialProfileRepository.findById(request.getReportsToUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supervisor not found with id: " + request.getReportsToUserId()));
            profile.setReportsTo(supervisor);
        }

        GovernmentOfficialProfile savedProfile = officialProfileRepository.save(profile);
        return mapToResponse(savedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public GovernmentOfficialProfileResponse getProfile(UUID userId) {
        GovernmentOfficialProfile profile = officialProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Official profile not found for user id: " + userId));

        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public GovernmentOfficialProfileResponse updateProfile(UUID userId, GovernmentOfficialProfileRequest request) {
        GovernmentOfficialProfile profile = officialProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Official profile not found for user id: " + userId));

        // Base UserProfile fields
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

        // Official fields
        profile.setDepartmentName(request.getDepartmentName());
        profile.setDepartmentCategory(request.getDepartmentCategory());
        profile.setDesignation(request.getDesignation());
        profile.setEmployeeId(request.getEmployeeId());
        profile.setOfficialPhone(request.getOfficialPhone());
        profile.setHierarchyLevel(request.getHierarchyLevel());
        profile.setDutyRadiusKm(request.getDutyRadiusKm());
        profile.setJurisdictionCode(request.getJurisdictionCode());

        if (request.getReportsToUserId() != null) {
            GovernmentOfficialProfile supervisor = officialProfileRepository.findById(request.getReportsToUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supervisor not found with id: " + request.getReportsToUserId()));
            profile.setReportsTo(supervisor);
        } else {
            profile.setReportsTo(null);
        }

        GovernmentOfficialProfile updatedProfile = officialProfileRepository.save(profile);
        return mapToResponse(updatedProfile);
    }

    @Override
    @Transactional
    public void deleteProfile(UUID userId) {
        if (!officialProfileRepository.existsById(userId)) {
            throw new UserNotFoundException("Official profile not found for user id: " + userId);
        }
        officialProfileRepository.deleteById(userId);
    }

    // Helper: Manual DTO -> Entity mapping to bypass ModelMapper issues
    private GovernmentOfficialProfile mapToEntity(GovernmentOfficialProfileRequest request) {
        GovernmentOfficialProfile profile = new GovernmentOfficialProfile();
        profile.setName(request.getName());
        profile.setAddressLine(request.getAddressLine());
        profile.setCity(request.getCity());
        profile.setState(request.getState());
        profile.setDistrict(request.getDistrict());
        profile.setPincode(request.getPincode());
        profile.setLatitude(request.getLatitude());
        profile.setLongitude(request.getLongitude());
        profile.setProfilePhotoUrl(request.getProfilePhotoUrl());

        profile.setDepartmentName(request.getDepartmentName());
        profile.setDepartmentCategory(request.getDepartmentCategory());
        profile.setDesignation(request.getDesignation());
        profile.setEmployeeId(request.getEmployeeId());
        profile.setOfficialPhone(request.getOfficialPhone());
        profile.setHierarchyLevel(request.getHierarchyLevel());
        profile.setDutyRadiusKm(request.getDutyRadiusKm() != null ? request.getDutyRadiusKm() : 25.0);
        profile.setJurisdictionCode(request.getJurisdictionCode());
        return profile;
    }

    // Helper: Entity -> Response DTO mapping
    private GovernmentOfficialProfileResponse mapToResponse(GovernmentOfficialProfile profile) {
        GovernmentOfficialProfileResponse response = new GovernmentOfficialProfileResponse();

        // Base profile fields
        response.setId(profile.getId());
        response.setName(profile.getName());
        response.setAddressLine(profile.getAddressLine());
        response.setCity(profile.getCity());
        response.setState(profile.getState());
        response.setDistrict(profile.getDistrict());
        response.setPincode(profile.getPincode());
        response.setLatitude(profile.getLatitude());
        response.setLongitude(profile.getLongitude());
        response.setProfilePhotoUrl(profile.getProfilePhotoUrl());

        // Official specific fields
        response.setDepartmentName(profile.getDepartmentName());
        response.setDepartmentCategory(profile.getDepartmentCategory());
        response.setDesignation(profile.getDesignation());
        response.setEmployeeId(profile.getEmployeeId());
        response.setOfficialPhone(profile.getOfficialPhone());
        response.setHierarchyLevel(profile.getHierarchyLevel());
        response.setStatus(profile.getStatus());
        response.setDutyRadiusKm(profile.getDutyRadiusKm());
        response.setIsVerified(profile.getIsVerified());
        response.setJurisdictionCode(profile.getJurisdictionCode());
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());

        if (profile.getReportsTo() != null) {
            response.setReportsToUserId(profile.getReportsTo().getId());
        }

        return response;
    }
}