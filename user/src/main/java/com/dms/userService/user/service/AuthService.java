package com.dms.userService.user.service;

import com.dms.userService.user.dto.request.AuthRequest;
import com.dms.userService.user.dto.request.RegisterRequest;
import com.dms.userService.user.dto.response.AuthResponse;
import com.dms.userService.user.dto.response.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    AuthResponse login(AuthRequest request);

    void logout(String authorizationHeader);
}