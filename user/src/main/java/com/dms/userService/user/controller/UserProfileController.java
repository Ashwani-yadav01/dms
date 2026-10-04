package com.dms.userService.user.controller;

import com.dms.userService.user.dto.request.UserProfileRequest;
import com.dms.userService.user.dto.response.UserProfileResponse;
import com.dms.userService.user.service.UserProfileFacadeService;
import com.dms.userService.user.security.JwtService;
import com.dms.userService.user.exception.BadRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileFacadeService profileFacadeService;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<UserProfileResponse> createProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody UserProfileRequest request) {
        return new ResponseEntity<>(profileFacadeService.createProfile(resolveUserId(userId, authorization), request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(profileFacadeService.getProfile(resolveUserId(userId, authorization)));
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(profileFacadeService.updateProfile(resolveUserId(userId, authorization), request));
    }

    @PatchMapping("/photo")
    public ResponseEntity<UserProfileResponse> updateProfilePhoto(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam String profilePhotoUrl) {
        return ResponseEntity.ok(profileFacadeService.updatePhoto(resolveUserId(userId, authorization), profilePhotoUrl));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("Authorization") String authorization) {
        profileFacadeService.deleteProfile(resolveUserId(userId, authorization));
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(UUID forwardedUserId, String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer "))
            throw new org.springframework.security.access.AccessDeniedException("You are not authorized to perform this action.");
        try {
            String token = authorization.substring(7);
            String tokenUserId = jwtService.extractClaim(token, claims -> claims.get("userId", String.class));
            UUID authenticatedId = tokenUserId == null ? UUID.fromString(jwtService.extractUsername(token)) : UUID.fromString(tokenUserId);
            if (!authenticatedId.equals(forwardedUserId))
                throw new org.springframework.security.access.AccessDeniedException("You are not authorized to perform this action.");
            return authenticatedId;
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new org.springframework.security.access.AccessDeniedException("You are not authorized to perform this action.");
        }
    }
}
