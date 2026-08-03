package com.dms.userService.user.controller;

import com.dms.userService.user.dto.request.UserProfileRequest;
import com.dms.userService.user.dto.response.UserProfileResponse;
import com.dms.userService.user.service.UserProfileFacadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileFacadeService profileFacadeService;

    @PostMapping
    public ResponseEntity<UserProfileResponse> createProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UserProfileRequest request) {
        return new ResponseEntity<>(profileFacadeService.createProfile(userId, request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileFacadeService.getProfile(userId));
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(profileFacadeService.updateProfile(userId, request));
    }

    @PatchMapping("/photo")
    public ResponseEntity<UserProfileResponse> updateProfilePhoto(
            @PathVariable UUID userId,
            @RequestParam String profilePhotoUrl) {
        return ResponseEntity.ok(profileFacadeService.updatePhoto(userId, profilePhotoUrl));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteProfile(@PathVariable UUID userId) {
        profileFacadeService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }
}