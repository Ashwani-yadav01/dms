package com.dms.userService.user.service;

import com.dms.userService.user.dto.request.RegisterRequest;
import com.dms.userService.user.dto.request.UserRequest;
import com.dms.userService.user.dto.response.RegisterResponse;
import com.dms.userService.user.dto.response.UserResponse;
import java.util.List;
import java.util.UUID;

public interface UserService {

    RegisterResponse createUser(RegisterRequest request);

    UserResponse getUserById(UUID userId);

    UserResponse getUserByEmail(String email);

    UserResponse getUserByMobile(String mobileNumber);

    List<UserResponse> getAllUsers();

    UserResponse updateUserRole(UUID userId, String role);

    String deleteUser(UUID userId);
}
