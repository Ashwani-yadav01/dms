package com.dms.userService.user.controller;

import com.dms.userService.user.dto.request.CreateDistrictAdminInvitationRequest;
import com.dms.userService.user.entity.DistrictAdminRegistry;
import com.dms.userService.user.entity.DistrictAdminRegistryStatus;
import com.dms.userService.user.entity.Role;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.exception.BadRequestException;
import com.dms.userService.user.repository.DistrictAdminRegistryRepository;
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
@RequestMapping("/api/v1/admin/district-admins")
@RequiredArgsConstructor
public class DistrictAdminRegistryAdminController {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final DistrictAdminRegistryRepository registryRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @GetMapping
    public List<DistrictAdminRegistry> list(@RequestHeader("Authorization") String authorization) {
        requireSuperAdmin(authorization);
        return registryRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @PostMapping
    public ResponseEntity<DistrictAdminRegistry> create(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CreateDistrictAdminInvitationRequest request) {
        requireSuperAdmin(authorization);

        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getMobileNumber().trim();
        if (userRepository.existsByEmail(email) || registryRepository.existsByAdminEmailIgnoreCase(email))
            throw new BadRequestException("An account or district admin invitation already uses this email.");
        if (userRepository.existsByMobileNumber(phone))
            throw new BadRequestException("An account already uses this mobile number.");
        if (registryRepository.existsByAdminPhone(phone))
            throw new BadRequestException("A district admin invitation already uses this mobile number.");

        DistrictAdminRegistry registry = new DistrictAdminRegistry();
        registry.setAdminEmail(email);
        registry.setAdminPhone(phone);
        registry.setDistrictName(request.getDistrictName());
        registry.setDepartmentCategory(request.getDepartmentCategory());
        registry.setEmployeeId(generateEmployeeId());
        registry.setAuthorizationCode(generateCode());
        registry.setStatus(DistrictAdminRegistryStatus.ACTIVE);
        return ResponseEntity.status(HttpStatus.CREATED).body(registryRepository.save(registry));
    }

    @PatchMapping("/{id}/revoke")
    public ResponseEntity<DistrictAdminRegistry> revoke(
            @RequestHeader("Authorization") String authorization,
            @PathVariable UUID id) {
        requireSuperAdmin(authorization);
        DistrictAdminRegistry registry = registryRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("District admin invitation not found: " + id));
        if (registry.getStatus() == DistrictAdminRegistryStatus.CONSUMED)
            throw new BadRequestException("Cannot revoke an invitation that has already been used.");
        registry.setStatus(DistrictAdminRegistryStatus.REVOKED);
        return ResponseEntity.ok(registryRepository.save(registry));
    }

    private String generateEmployeeId() {
        StringBuilder id = new StringBuilder("DMS-DIST-");
        for (int i = 0; i < 8; i++) id.append(RANDOM.nextInt(10));
        String candidate = id.toString();
        return registryRepository.existsByEmployeeId(candidate) ? generateEmployeeId() : candidate;
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder("DIST-");
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
