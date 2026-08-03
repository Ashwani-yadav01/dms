package com.dms.userService.user.service;

import com.dms.userService.user.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse getUserById(UUID userId);

    UserResponse getUserByEmail(String email);

    UserResponse getUserByMobile(String mobileNumber);

//    Page<UserResponse> getAllUsers(Pageable pageable);
       List<UserResponse> getAllUsers();

    void deleteUser(UUID userId);
}