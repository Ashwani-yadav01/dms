package com.dms.userService.user.controller;

import com.dms.userService.user.dto.request.SafetyCheckInRequest;
import com.dms.userService.user.dto.response.SafetyCheckInResponse;
import com.dms.userService.user.security.JwtService;
import com.dms.userService.user.service.PersonalSafetyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/safety-checkins")
@RequiredArgsConstructor
public class SafetyCheckInController {

    private final PersonalSafetyService personalSafetyService;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<SafetyCheckInResponse> recordCheckIn(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody SafetyCheckInRequest request) {
        UUID authenticatedId = resolveUserId(userId, authorization);
        return new ResponseEntity<>(personalSafetyService.recordCheckIn(authenticatedId, request), HttpStatus.CREATED);
    }

    @GetMapping("/latest")
    public ResponseEntity<SafetyCheckInResponse> getLatestCheckIn(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UUID authenticatedId = resolveUserId(userId, authorization);
        SafetyCheckInResponse latest = personalSafetyService.getLatestCheckIn(authenticatedId);
        return latest != null ? ResponseEntity.ok(latest) : ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<SafetyCheckInResponse>> getCheckInHistory(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UUID authenticatedId = resolveUserId(userId, authorization);
        return ResponseEntity.ok(personalSafetyService.getCheckInHistory(authenticatedId));
    }

    private UUID resolveUserId(UUID forwardedUserId, String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("Authorization required.");
        }
        try {
            String token = authorization.substring(7);
            String tokenUserId = jwtService.extractClaim(token, claims -> claims.get("userId", String.class));
            UUID authenticatedId = tokenUserId != null ? UUID.fromString(tokenUserId) : UUID.fromString(jwtService.extractUsername(token));
            if (forwardedUserId != null && !authenticatedId.equals(forwardedUserId)) {
                throw new AccessDeniedException("User ID mismatch.");
            }
            return authenticatedId;
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new AccessDeniedException("Invalid authentication credentials.");
        }
    }
}
