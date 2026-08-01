package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.UserRequest;
import com.dms.userService.user.dto.response.UserResponse;
import com.dms.userService.user.service.UserService;

import java.util.List;
import java.util.UUID;

public class UserServiceImpl implements UserService {
    @Override
    public UserResponse createUser(UserRequest request) {
        return null;
    }

    @Override
    public UserResponse getUserById(UUID userId) {
        return null;
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        return null;
    }

    @Override
    public UserResponse getUserByMobile(String mobileNumber) {
        return null;
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return List.of();
    }

    @Override
    public UserResponse updateUserRole(UUID userId, String role) {
        return null;
    }

    @Override
    public void deleteUser(UUID userId) {

    }
}
