package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.*;
import com.dms.userService.user.dto.response.*;

import com.dms.userService.user.exception.UserAlreadyExistsException;
import com.dms.userService.user.exception.UserNotFoundException;
import com.dms.userService.user.exception.BadRequestException;
import com.dms.userService.user.repository.UserProfileRepository;
import com.dms.userService.user.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileFacadeServiceImpl implements UserProfileFacadeService {

    private final UserProfileRepository userProfileRepository;

    // Role-specific sub-services
    private final CitizenProfileService citizenProfileService;
    private final VolunteerProfileService volunteerProfileService;
    private final NGOProfileService ngoProfileService;
    private final GovernmentOfficialService governmentOfficialService;

    @Override
    @Transactional
    public UserProfileResponse createProfile(UUID userId, UserProfileRequest request) {
        // Prevent duplicate profile creation
        if (userProfileRepository.existsById(userId)) {
            throw new UserAlreadyExistsException("Profile already exists for user ID: " + userId);
        }

        return delegateToRoleServiceCreate(userId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        // First check if a profile exists at all
        if (!userProfileRepository.existsById(userId)) {
            throw new UserNotFoundException("Profile not found for user ID: " + userId);
        }

        // Delegate retrieval based on which subtype exists in DB
        if (citizenProfileService.existsById(userId)) {
            return citizenProfileService.getProfile(userId);
        } else if (volunteerProfileService.existsById(userId)) {
            return volunteerProfileService.getProfile(userId);
        } else if (ngoProfileService.existsById(userId)) {
            return ngoProfileService.getProfile(userId);
        } else if (governmentOfficialService.existsById(userId)) {
            return governmentOfficialService.getProfile(userId);
        }

        throw new UserNotFoundException("No specific profile role found for user ID: " + userId);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UserProfileRequest request) {
        if (!userProfileRepository.existsById(userId)) {
            throw new UserNotFoundException("Cannot update. Profile does not exist for user ID: " + userId);
        }

        // Handles profile updates (and role transitions if the request subtype changed)
        return delegateToRoleServiceUpdate(userId, request);
    }

    @Override
    @Transactional
    public UserProfileResponse updatePhoto(UUID userId, String profilePhotoUrl) {
        var profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Profile not found for user ID: " + userId));

        profile.setProfilePhotoUrl(profilePhotoUrl);
        userProfileRepository.save(profile);

        return getProfile(userId);
    }

    @Override
    @Transactional
    public void deleteProfile(UUID userId) {
        if (!userProfileRepository.existsById(userId)) {
            throw new UserNotFoundException("Cannot delete. Profile does not exist for user ID: " + userId);
        }

        // Sub-services handle cascading or cleaning up specific relations before deletion
        if (citizenProfileService.existsById(userId)) {
            citizenProfileService.deleteProfile(userId);
        } else if (volunteerProfileService.existsById(userId)) {
            volunteerProfileService.deleteProfile(userId);
        } else if (ngoProfileService.existsById(userId)) {
            ngoProfileService.deleteProfile(userId);
        } else if (governmentOfficialService.existsById(userId)) {
            governmentOfficialService.deleteProfile(userId);
        } else {
            userProfileRepository.deleteById(userId);
        }
    }

    // Helper method to route polymorphic creation payloads
    private UserProfileResponse delegateToRoleServiceCreate(UUID userId, UserProfileRequest request) {
        return switch (request) {
            case CitizenProfileRequest req -> citizenProfileService.createProfile(userId, req);
            case VolunteerProfileRequest req -> volunteerProfileService.createProfile(userId, req);
            case NGOProfileRequest req -> ngoProfileService.createProfile(userId, req);
            case GovernmentOfficialProfileRequest req -> governmentOfficialService.createProfile(userId, req);
            default -> throw new BadRequestException("Unsupported profile type: " + request.getClass().getSimpleName());
        };
    }

    // Helper method to route polymorphic update payloads
    private UserProfileResponse delegateToRoleServiceUpdate(UUID userId, UserProfileRequest request) {
        return switch (request) {
            case CitizenProfileRequest req -> citizenProfileService.updateProfile(userId, req);
            case VolunteerProfileRequest req -> volunteerProfileService.updateProfile(userId, req);
            case NGOProfileRequest req -> ngoProfileService.updateProfile(userId, req);
            case GovernmentOfficialProfileRequest req -> governmentOfficialService.updateProfile(userId, req);
            default -> throw new BadRequestException("Unsupported profile type: " + request.getClass().getSimpleName());
        };
    }
}