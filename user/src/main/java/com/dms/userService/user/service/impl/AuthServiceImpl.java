package com.dms.userService.user.service.impl;

import com.dms.userService.user.dto.request.AuthRequest;
import com.dms.userService.user.dto.request.RegisterRequest;
import com.dms.userService.user.dto.response.AuthResponse;
import com.dms.userService.user.dto.response.RegisterResponse;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.entity.Role;
import com.dms.userService.user.entity.GovernmentRegistryStatus;
import com.dms.userService.user.entity.HospitalRegistryStatus;
import com.dms.userService.user.exception.BadRequestException;
import com.dms.userService.user.repository.GovernmentRegistryRepository;
import com.dms.userService.user.repository.GovernmentOfficialProfileRepository;
import com.dms.userService.user.repository.HospitalRegistryRepository;
import com.dms.userService.user.repository.RescueTeamProfileRepository;
import com.dms.userService.user.exception.UserAlreadyExistsException;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.security.CustomUserDetails;
import com.dms.userService.user.security.JwtService;
import com.dms.userService.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate redisTemplate;
    private final GovernmentRegistryRepository governmentRegistryRepository;
    private final GovernmentOfficialProfileRepository governmentOfficialProfileRepository;
    private final HospitalRegistryRepository hospitalRegistryRepository;
    private final RescueTeamProfileRepository rescueTeamProfileRepository;
    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (request.getRole() == Role.GOVERNMENT_OFFICIAL) {
            boolean authorized = request.getAuthorizationCode() != null
                    && governmentRegistryRepository.findByAuthorizationCode(request.getAuthorizationCode().trim().toUpperCase())
                    .filter(record -> record.getStatus() == GovernmentRegistryStatus.ACTIVE)
                    .filter(record -> record.getOfficialEmail() == null || record.getOfficialEmail().equalsIgnoreCase(request.getEmail().trim()))
                    .filter(record -> record.getOfficialPhone() == null || record.getOfficialPhone().equals(request.getMobileNumber().trim()))
                    .isPresent();
            if (!authorized) throw new BadRequestException("Government official authorization failed.");
        } else if (request.getRole() == Role.HOSPITAL) {
            boolean authorized = request.getAuthorizationCode() != null
                    && hospitalRegistryRepository.findByAuthorizationCode(request.getAuthorizationCode().trim().toUpperCase())
                    .filter(record -> record.getStatus() == HospitalRegistryStatus.ACTIVE)
                    .filter(record -> record.getHospitalEmail() == null || record.getHospitalEmail().equalsIgnoreCase(request.getEmail().trim()))
                    .filter(record -> record.getHospitalPhone() == null || record.getHospitalPhone().equals(request.getMobileNumber().trim()))
                    .isPresent();
            if (!authorized) throw new BadRequestException("Hospital authorization failed. A valid hospital invitation code is required.");
        } else if (request.getRole() == Role.DISTRICT_ADMIN || request.getRole() == Role.RESCUE_TEAM || request.getRole() == Role.SUPER_ADMIN) {
            throw new BadRequestException("This profile must be provisioned by an administrator.");
        }
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

        // 3. Consume the hospital invitation so the code cannot be reused
        if (request.getRole() == Role.HOSPITAL && request.getAuthorizationCode() != null) {
            hospitalRegistryRepository.findByAuthorizationCode(request.getAuthorizationCode().trim().toUpperCase())
                    .ifPresent(registry -> {
                        registry.setStatus(HospitalRegistryStatus.CONSUMED);
                        hospitalRegistryRepository.save(registry);
                    });
        }

        // 4. Generate JWT Token immediately upon registration
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", savedUser.getId());
        extraClaims.put("role", savedUser.getRole().name());
        extraClaims.put("govVerified", false);
        extraClaims.put("rescueVerified", false);
        extraClaims.put("hospitalVerified", false);

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
        governmentOfficialProfileRepository.findById(user.getId()).ifPresent(profile -> {
            extraClaims.put("govVerified", Boolean.TRUE.equals(profile.getIsVerified()));
            extraClaims.put("officialStatus", profile.getStatus() == null ? null : profile.getStatus().name());
            extraClaims.put("officialLatitude", profile.getDutyOfficeLatitude());
            extraClaims.put("officialLongitude", profile.getDutyOfficeLongitude());
            extraClaims.put("officialDutyRadiusKm", profile.getDutyRadiusKm());
        });
        rescueTeamProfileRepository.findById(user.getId()).ifPresent(profile -> {
            extraClaims.put("rescueVerified", Boolean.TRUE.equals(profile.getIsVerified()));
            extraClaims.put("rescueDepartmentId", profile.getDepartmentId() == null ? null : profile.getDepartmentId().toString());
        });

        String jwtToken = jwtService.generateToken(extraClaims, userDetails);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .userId(user.getId())
                .role(user.getRole().name())
                .profileCompleted(user.isProfileCompleted())
                .build();
    }
    @Override
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }

        String token = authorizationHeader.substring(7);

        // 1. Calculate remaining time on the token
        Date expirationDate = jwtService.extractExpiration(token);
        long remainingTimeMillis = expirationDate.getTime() - System.currentTimeMillis();

        // 2. If token is still valid, push it to the Redis blacklist
        if (remainingTimeMillis > 0) {
            String blacklistKey = "jwt_blacklist:" + token;
            redisTemplate.opsForValue().set(
                    blacklistKey,
                    "revoked",
                    remainingTimeMillis,
                    TimeUnit.MILLISECONDS
            );
        }
    }
}
