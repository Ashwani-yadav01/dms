package com.dms.userService.user.controller;

import com.dms.userService.user.dto.request.UserProfileRequest;
import com.dms.userService.user.dto.response.UserProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/users/{userId}/profile")
public class UserProfileController {

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getProfile(
            @PathVariable UUID userId) {
        return null;
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @PathVariable UUID userId,
            @RequestBody UserProfileRequest request,
            @RequestParam String role) {
        return null;
    }

    @PutMapping("/photo")
    public ResponseEntity<UserProfileResponse> updateProfilePhoto(
            @PathVariable UUID userId,
            @RequestParam String profilePhotoUrl) {
        return null;
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteProfile(
            @PathVariable UUID userId) {
        return null;
    }
}