package com.dms.userService.user.controller;

import com.dms.userService.user.dto.request.TrustedContactRequest;
import com.dms.userService.user.dto.response.TrustedContactResponse;
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
@RequestMapping("/api/v1/users/trusted-contacts")
@RequiredArgsConstructor
public class TrustedContactController {

    private final PersonalSafetyService personalSafetyService;
    private final JwtService jwtService;

    @GetMapping
    public ResponseEntity<List<TrustedContactResponse>> getMyContacts(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UUID authenticatedId = resolveUserId(userId, authorization);
        return ResponseEntity.ok(personalSafetyService.getTrustedContacts(authenticatedId));
    }

    @PostMapping
    public ResponseEntity<TrustedContactResponse> addContact(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody TrustedContactRequest request) {
        UUID authenticatedId = resolveUserId(userId, authorization);
        return new ResponseEntity<>(personalSafetyService.addTrustedContact(authenticatedId, request), HttpStatus.CREATED);
    }

    @PutMapping("/{contactId}")
    public ResponseEntity<TrustedContactResponse> updateContact(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID contactId,
            @Valid @RequestBody TrustedContactRequest request) {
        UUID authenticatedId = resolveUserId(userId, authorization);
        return ResponseEntity.ok(personalSafetyService.updateTrustedContact(authenticatedId, contactId, request));
    }

    @DeleteMapping("/{contactId}")
    public ResponseEntity<Void> deleteContact(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID contactId) {
        UUID authenticatedId = resolveUserId(userId, authorization);
        personalSafetyService.deleteTrustedContact(authenticatedId, contactId);
        return ResponseEntity.noContent().build();
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
