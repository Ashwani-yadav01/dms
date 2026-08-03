package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.AuthRequest;
import com.dms.userService.user.dto.request.RegisterRequest;
import com.dms.userService.user.dto.response.AuthResponse;
import com.dms.userService.user.dto.response.RegisterResponse;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.exception.UserAlreadyExistsException;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.security.CustomUserDetails;
import com.dms.userService.user.security.JwtService;
import com.dms.userService.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        if (userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new UserAlreadyExistsException("Mobile number already registered: " + request.getMobileNumber());
        }

        // 1. Create and Save User Entity
        User user = new User();
        user.setEmail(request.getEmail());
        user.setMobileNumber(request.getMobileNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setProfileCompleted(false);

        User savedUser = userRepository.save(user);

        // 2. Generate JWT Token immediately upon registration
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", savedUser.getId());
        extraClaims.put("role", savedUser.getRole().name());

        String jwtToken = jwtService.generateToken(extraClaims, userDetails);

        // 3. Return response with token & profileCompleted status
        return RegisterResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .profileCompleted(savedUser.isProfileCompleted())
                .accessToken(jwtToken)
                .message("User registered successfully. Please proceed to complete your profile.")
                .build();
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("role", user.getRole().name());

        String jwtToken = jwtService.generateToken(extraClaims, userDetails);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .userId(user.getId())
                .role(user.getRole().name())
                .profileCompleted(user.isProfileCompleted())
                .build();
    }
}