package com.dms.userService.user.controller;

import com.dms.userService.user.dto.request.CreateGovernmentOfficialRequest;
import com.dms.userService.user.dto.response.AdminUserSummary;
import com.dms.userService.user.entity.GovernmentRegistry;
import com.dms.userService.user.entity.GovernmentRegistryStatus;
import com.dms.userService.user.entity.Role;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.exception.BadRequestException;
import com.dms.userService.user.repository.GovernmentRegistryRepository;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class GovernmentOfficialAdminController {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final GovernmentRegistryRepository registryRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @GetMapping("/government-officials")
    public List<GovernmentRegistry> list(@RequestHeader("Authorization") String authorization) {
        requireSuperAdmin(authorization);
        return registryRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @GetMapping("/users")
    public List<AdminUserSummary> listUsers(@RequestHeader("Authorization") String authorization) {
        requireSuperAdmin(authorization);
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(user -> {
                    GovernmentRegistry registry = registryRepository.findByOfficialEmailIgnoreCase(user.getEmail()).orElse(null);
                    return new AdminUserSummary(user.getId(), user.getEmail(), user.getMobileNumber(),
                            user.getRole(), user.isProfileCompleted(), user.getCreatedAt(),
                            registry == null ? null : registry.getEmployeeId(),
                            registry == null ? null : registry.getAuthorizationCode());
                })
                .toList();
    }

    @PostMapping("/government-officials")
    public ResponseEntity<GovernmentRegistry> create(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CreateGovernmentOfficialRequest request) {
        requireSuperAdmin(authorization);

        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getMobileNumber().trim();
        if (userRepository.existsByEmail(email) || registryRepository.existsByOfficialEmailIgnoreCase(email))
            throw new BadRequestException("An account or official profile already uses this email.");
        if (userRepository.existsByMobileNumber(phone))
            throw new BadRequestException("An account already uses this mobile number.");
        if (registryRepository.existsByOfficialPhone(phone))
            throw new BadRequestException("An official profile already uses this mobile number.");
        GovernmentRegistry registry = new GovernmentRegistry();
        registry.setOfficialEmail(email);
        registry.setOfficialPhone(phone);
        registry.setEmployeeId(generateEmployeeId());
        registry.setAuthorizationCode(generateCode());
        registry.setDutyRadiusKm(20.0);
        registry.setStatus(GovernmentRegistryStatus.ACTIVE);
        return ResponseEntity.status(HttpStatus.CREATED).body(registryRepository.save(registry));
    }

    private String generateEmployeeId() {
        StringBuilder id = new StringBuilder("DMS-GOV-");
        for (int i = 0; i < 8; i++) id.append(RANDOM.nextInt(10));
        String candidate = id.toString();
        return registryRepository.existsByEmployeeId(candidate) ? generateEmployeeId() : candidate;
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder("GOV-");
        for (int i = 0; i < 12; i++) code.append(CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)]);
        String candidate = code.toString();
        return registryRepository.existsByAuthorizationCode(candidate) ? generateCode() : candidate;
    }

    private void requireSuperAdmin(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer "))
            throw new AccessDeniedException("You are not authorized to perform this action.");
        try {
            String token = authorization.substring(7);
            String id = jwtService.extractClaim(token, claims -> claims.get("userId", String.class));
            UUID actorId = UUID.fromString(id);
            User actor = userRepository.findById(actorId)
                    .orElseThrow(() -> new AccessDeniedException("You are not authorized to perform this action."));
            if (actor.getRole() != Role.SUPER_ADMIN)
                throw new AccessDeniedException("You are not authorized to perform this action.");
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new AccessDeniedException("You are not authorized to perform this action.");
        }
    }
}
