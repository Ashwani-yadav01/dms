package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.GovernmentOfficialProfileRequest;
import com.dms.userService.user.dto.response.GovernmentOfficialProfileResponse;
import com.dms.userService.user.entity.GovernmentOfficialProfile;
import com.dms.userService.user.entity.OfficialStatus;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.entity.GovernmentRegistry;
import com.dms.userService.user.entity.GovernmentRegistryStatus;
import com.dms.userService.user.exception.BadRequestException;
import com.dms.userService.user.exception.ResourceNotFoundException;
import com.dms.userService.user.exception.UserAlreadyExistsException;
import com.dms.userService.user.exception.UserNotFoundException;
import com.dms.userService.user.repository.GovernmentOfficialProfileRepository;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.repository.GovernmentRegistryRepository;
import com.dms.userService.user.service.GovernmentOfficialProfileService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class GovernmentOfficialProfileServiceImpl implements GovernmentOfficialProfileService {

    private final GovernmentOfficialProfileRepository officialProfileRepository;
    private final GovernmentRegistryRepository registryRepository;
    private final UserRepository userRepository;
    private final ModelMapper mapper;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] PROFILE_ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

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
            throw new UserAlreadyExistsException("Official with employee ID already exists");
        }

        // 1. Instantiate and map fields explicitly to prevent ModelMapper converter bugs
        GovernmentOfficialProfile profile = mapToEntity(request);
        profile.setGovernmentProfileId(generateGovernmentProfileId());

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

        GovernmentRegistry registry = registryRepository.findByEmployeeId(request.getEmployeeId()).orElse(null);
        if (registry == null || registry.getStatus() != GovernmentRegistryStatus.ACTIVE
                || !registry.matches(request)
                || (registry.getOfficialEmail() != null && !registry.getOfficialEmail().equalsIgnoreCase(user.getEmail()))
                || (registry.getOfficialPhone() != null && !registry.getOfficialPhone().equals(user.getMobileNumber()))) {
            throw new BadRequestException("Government official verification failed.");
        }
        applyRegistryProfileDetails(registry, request);
        profile.setIsVerified(true);
        applyDutyArea(profile, registry);
        GovernmentOfficialProfile savedProfile = officialProfileRepository.save(profile);
        registry.setGovernmentProfileId(savedProfile.getGovernmentProfileId());
        registryRepository.save(registry);
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

        boolean criticalFieldsChanged = !same(profile.getEmployeeId(), request.getEmployeeId())
                || !same(profile.getDepartmentName(), request.getDepartmentName())
                || !same(profile.getDesignation(), request.getDesignation())
                || profile.getHierarchyLevel() != request.getHierarchyLevel();
        GovernmentRegistry registry = registryRepository.findByEmployeeId(request.getEmployeeId()).orElse(null);
        if (registry == null || registry.getStatus() != GovernmentRegistryStatus.ACTIVE
                || !registry.matches(request)
                || (registry.getOfficialEmail() != null && !registry.getOfficialEmail().equalsIgnoreCase(profile.getUser().getEmail()))
                || (registry.getOfficialPhone() != null && !registry.getOfficialPhone().equals(profile.getUser().getMobileNumber()))) {
            throw new BadRequestException("Government official verification failed.");
        }

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
        profile.setDepartmentId(request.getDepartmentId());
        profile.setDepartmentCategory(request.getDepartmentCategory());
        profile.setDesignation(request.getDesignation());
        profile.setEmployeeId(request.getEmployeeId());
        profile.setOfficialPhone(request.getOfficialPhone());
        profile.setHierarchyLevel(request.getHierarchyLevel());
        profile.setJurisdictionCode(request.getJurisdictionCode());
        applyRegistryProfileDetails(registry, request);
        applyDutyArea(profile, registry);
        if (criticalFieldsChanged) profile.setIsVerified(true);

        if (request.getReportsToUserId() != null) {
            GovernmentOfficialProfile supervisor = officialProfileRepository.findById(request.getReportsToUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supervisor not found with id: " + request.getReportsToUserId()));
            profile.setReportsTo(supervisor);
        } else {
            profile.setReportsTo(null);
        }

        GovernmentOfficialProfile updatedProfile = officialProfileRepository.save(profile);
        registryRepository.save(registry);
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
        // Never map verification state or identifiers from client data.
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
        profile.setDepartmentId(request.getDepartmentId());
        profile.setDepartmentCategory(request.getDepartmentCategory());
        profile.setDesignation(request.getDesignation());
        profile.setEmployeeId(request.getEmployeeId());
        profile.setOfficialPhone(request.getOfficialPhone());
        profile.setHierarchyLevel(request.getHierarchyLevel());
        profile.setDutyRadiusKm(40.0);
        profile.setJurisdictionCode(request.getJurisdictionCode());
        return profile;
    }

    private String generateGovernmentProfileId() {
        for (int attempt = 0; attempt < 10; attempt++) {
            int numericPart = SECURE_RANDOM.nextInt(900000) + 100000;
            StringBuilder suffix = new StringBuilder(6);
            for (int i = 0; i < 6; i++) suffix.append(PROFILE_ID_CHARS[SECURE_RANDOM.nextInt(PROFILE_ID_CHARS.length)]);
            String candidate = "GOV-" + numericPart + "-" + suffix;
            if (!officialProfileRepository.existsByGovernmentProfileId(candidate)) return candidate;
        }
        throw new IllegalStateException("Unable to generate a unique government profile identifier");
    }

    private boolean same(String left, String right) {
        return left == null ? right == null : right != null && left.equalsIgnoreCase(right.trim());
    }

    private void applyRegistryProfileDetails(GovernmentRegistry registry, GovernmentOfficialProfileRequest request) {
        registry.setDepartmentName(request.getDepartmentName());
        registry.setDesignation(request.getDesignation());
        registry.setHierarchyLevel(request.getHierarchyLevel());
        registry.setOfficeLatitude(request.getLatitude());
        registry.setOfficeLongitude(request.getLongitude());
        if (registry.getDutyRadiusKm() == null) registry.setDutyRadiusKm(20.0);
    }

    private void applyDutyArea(GovernmentOfficialProfile profile, GovernmentRegistry registry) {
        profile.setDutyOfficeLatitude(registry.getOfficeLatitude());
        profile.setDutyOfficeLongitude(registry.getOfficeLongitude());
        profile.setDutyRadiusKm(registry.getDutyRadiusKm());
    }

    // Helper: Entity -> Response DTO mapping
    private GovernmentOfficialProfileResponse mapToResponse(GovernmentOfficialProfile profile) {
        GovernmentOfficialProfileResponse response = new GovernmentOfficialProfileResponse();

        // Base profile fields
        response.setId(profile.getId());
        response.setGovernmentProfileId(profile.getGovernmentProfileId());
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
        response.setDepartmentId(profile.getDepartmentId());
        response.setDepartmentCategory(profile.getDepartmentCategory());
        response.setDesignation(profile.getDesignation());
        response.setEmployeeId(profile.getEmployeeId());
        response.setOfficialPhone(profile.getOfficialPhone());
        response.setHierarchyLevel(profile.getHierarchyLevel());
        response.setStatus(profile.getStatus());
        response.setDutyRadiusKm(profile.getDutyRadiusKm());
        response.setIsVerified(profile.getIsVerified());
        response.setJurisdictionCode(profile.getJurisdictionCode());


        if (profile.getReportsTo() != null) {
            response.setReportsToUserId(profile.getReportsTo().getId());
        }

        return response;
    }
}
