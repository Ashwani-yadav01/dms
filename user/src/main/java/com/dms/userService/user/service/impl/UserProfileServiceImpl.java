package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.UserProfileRequest;
import com.dms.userService.user.dto.response.UserProfileResponse;
import com.dms.userService.user.entity.UserProfile;
import com.dms.userService.user.repository.UserProfileRepository;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.service.UserProfileService;
import com.dms.userService.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ModelMapper mapper;



    @Override
    public UserProfileResponse getProfile(UUID userId) {
        return null;
    }

    @Override
    public UserProfileResponse updateProfile(UUID userId, UserProfileRequest request) {
        return null;
    }

    @Override
    public UserProfileResponse updateProfilePhoto(UUID userId, String profilePhotoUrl) {
        return null;
    }

    @Override
    public void deleteProfile(UUID userId) {

    }
}
