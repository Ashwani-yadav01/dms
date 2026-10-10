package com.dms.userService.user.controller;

import com.dms.userService.user.dto.request.CreateHospitalInvitationRequest;
import com.dms.userService.user.entity.HospitalRegistry;
import com.dms.userService.user.entity.HospitalRegistryStatus;
import com.dms.userService.user.entity.Role;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.exception.BadRequestException;
import com.dms.userService.user.repository.HospitalRegistryRepository;
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

/**
 * Hospital invitation management. Only the Super Admin may issue, list, and
 * revoke hospital invitations. This is the single entry point that proves a
 * hospital user is authorized to register.
 */
@RestController
@RequestMapping("/api/v1/admin/hospitals")
@RequiredArgsConstructor
public class HospitalRegistryAdminController {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final HospitalRegistryRepository registryRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @GetMapping
    public List<HospitalRegistry> list(@RequestHeader("Authorization") String authorization) {
        requireSuperAdmin(authorization);
        return registryRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @GetMapping("/active")
    public List<HospitalRegistry> listActive(@RequestHeader("Authorization") String authorization) {
        requireSuperAdmin(authorization);
        return registryRepository.findByStatusOrderByCreatedAtDesc(HospitalRegistryStatus.ACTIVE);
    }

    @PostMapping
    public ResponseEntity<HospitalRegistry> create(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CreateHospitalInvitationRequest request) {
        requireSuperAdmin(authorization);

        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getMobileNumber().trim();
        if (userRepository.existsByEmail(email) || registryRepository.existsByHospitalEmailIgnoreCase(email))
            throw new BadRequestException("An account or hospital invitation already uses this email.");
        if (userRepository.existsByMobileNumber(phone))
            throw new BadRequestException("An account already uses this mobile number.");
        if (registryRepository.existsByHospitalPhone(phone))
            throw new BadRequestException("A hospital invitation already uses this mobile number.");

        HospitalRegistry registry = new HospitalRegistry();
        registry.setHospitalEmail(email);
        registry.setHospitalPhone(phone);
        registry.setHospitalName(request.getHospitalName());
        registry.setEmployeeId(generateEmployeeId());
        registry.setAuthorizationCode(generateCode());
        registry.setStatus(HospitalRegistryStatus.ACTIVE);
        return ResponseEntity.status(HttpStatus.CREATED).body(registryRepository.save(registry));
    }

    @PatchMapping("/{id}/revoke")
    public ResponseEntity<HospitalRegistry> revoke(
            @RequestHeader("Authorization") String authorization,
            @PathVariable UUID id) {
        requireSuperAdmin(authorization);
        HospitalRegistry registry = registryRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Hospital invitation not found: " + id));
        if (registry.getStatus() == HospitalRegistryStatus.CONSUMED)
            throw new BadRequestException("Cannot revoke an invitation that has already been used.");
        registry.setStatus(HospitalRegistryStatus.REVOKED);
        return ResponseEntity.ok(registryRepository.save(registry));
    }

    private String generateEmployeeId() {
        StringBuilder id = new StringBuilder("DMS-HOSP-");
        for (int i = 0; i < 8; i++) id.append(RANDOM.nextInt(10));
        String candidate = id.toString();
        return registryRepository.existsByEmployeeId(candidate) ? generateEmployeeId() : candidate;
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder("HOSP-");
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