package com.dms.userService.user.controller;

import com.dms.userService.user.dto.response.UserResponse;
import com.dms.userService.user.entity.Role;
import com.dms.userService.user.entity.User;
import com.dms.userService.user.exception.UserNotFoundException;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.security.JwtService;
import com.dms.userService.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable UUID userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-User-Role", required = false) String xUserRole) {

        User actor = resolveCaller(authorization, xUserId, xUserRole);
        if (!actor.getId().equals(userId) && actor.getRole() != Role.SUPER_ADMIN && actor.getRole() != Role.DISTRICT_ADMIN) {
            throw new AccessDeniedException("You are not authorized to view another user's account details.");
        }
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(
            @PathVariable String email,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-User-Role", required = false) String xUserRole) {

        User actor = resolveCaller(authorization, xUserId, xUserRole);
        if (!actor.getEmail().equalsIgnoreCase(email) && actor.getRole() != Role.SUPER_ADMIN && actor.getRole() != Role.DISTRICT_ADMIN) {
            throw new AccessDeniedException("You are not authorized to search users by email.");
        }
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/mobile/{mobileNumber}")
    public ResponseEntity<UserResponse> getUserByMobile(
            @PathVariable String mobileNumber,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-User-Role", required = false) String xUserRole) {

        User actor = resolveCaller(authorization, xUserId, xUserRole);
        if (!mobileNumber.equals(actor.getMobileNumber()) && actor.getRole() != Role.SUPER_ADMIN && actor.getRole() != Role.DISTRICT_ADMIN) {
            throw new AccessDeniedException("You are not authorized to search users by mobile number.");
        }
        return ResponseEntity.ok(userService.getUserByMobile(mobileNumber));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-User-Role", required = false) String xUserRole) {

        User actor = resolveCaller(authorization, xUserId, xUserRole);
        if (actor.getRole() != Role.SUPER_ADMIN && actor.getRole() != Role.DISTRICT_ADMIN) {
            throw new AccessDeniedException("You are not authorized to enumerate users.");
        }
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-User-Role", required = false) String xUserRole) {

        User actor = resolveCaller(authorization, xUserId, xUserRole);
        if (actor.getRole() != Role.SUPER_ADMIN) {
            throw new AccessDeniedException("Only SUPER_ADMIN is authorized to delete user accounts.");
        }
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/nearby/emails")
    public ResponseEntity<List<String>> getNearbyUserEmails(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5.0") Double radiusKm,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String xUserRole) {

        // Restrict external user calls arriving via Gateway: only administrative roles or internal calls permitted
        if (xUserRole != null) {
            Role role;
            try {
                role = Role.valueOf(xUserRole);
            } catch (Exception e) {
                throw new AccessDeniedException("Invalid role header.");
            }
            if (role != Role.SUPER_ADMIN && role != Role.DISTRICT_ADMIN && role != Role.GOVERNMENT_OFFICIAL) {
                throw new AccessDeniedException("You are not authorized to query nearby citizen emails.");
            }
        }

        List<String> emails = userService.findEmailsWithinRadius(latitude, longitude, radiusKm);
        return ResponseEntity.ok(emails);
    }

    private User resolveCaller(String authorization, String xUserId, String xUserRole) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                String token = authorization.substring(7);
                String tokenUserId = jwtService.extractClaim(token, claims -> claims.get("userId", String.class));
                UUID actorId = tokenUserId != null ? UUID.fromString(tokenUserId) : UUID.fromString(jwtService.extractUsername(token));
                return userRepository.findById(actorId)
                        .orElseThrow(() -> new UserNotFoundException("User not found"));
            } catch (UserNotFoundException e) {
                throw e;
            } catch (Exception e) {
                throw new AccessDeniedException("Invalid authorization token.");
            }
        }

        if (xUserId != null && !xUserId.isBlank()) {
            try {
                UUID actorId = UUID.fromString(xUserId);
                return userRepository.findById(actorId)
                        .orElseThrow(() -> new UserNotFoundException("User not found"));
            } catch (UserNotFoundException e) {
                throw e;
            } catch (Exception e) {
                throw new AccessDeniedException("Invalid user ID header.");
            }
        }

        throw new AccessDeniedException("Authorization required.");
    }
}