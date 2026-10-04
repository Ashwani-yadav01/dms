package com.dms.userService.user.controller;

import com.dms.userService.user.dto.response.RescueTeamProfileResponse;
import com.dms.userService.user.entity.Role;
import com.dms.userService.user.exception.UserNotFoundException;
import com.dms.userService.user.repository.UserRepository;
import com.dms.userService.user.security.JwtService;
import com.dms.userService.user.service.RescueTeamProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/admin/rescue-teams")
@RequiredArgsConstructor
public class RescueTeamAdminController {
    private final RescueTeamProfileService rescueTeamProfileService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @GetMapping("/{userId}")
    public RescueTeamProfileResponse getForReview(@PathVariable UUID userId, @RequestHeader("Authorization") String authorization) {
        return rescueTeamProfileService.getProfileForAdmin(userId, requireAdmin(authorization));
    }

    @PatchMapping("/{userId}/verify")
    public RescueTeamProfileResponse verify(@PathVariable UUID userId, @RequestHeader("Authorization") String authorization) {
        return rescueTeamProfileService.verifyProfile(userId, requireAdmin(authorization));
    }

    private UUID requireAdmin(String authorization) {
        UUID actorId = actorId(authorization);
        Role role = userRepository.findById(actorId).orElseThrow(() -> new UserNotFoundException("User not found")).getRole();
        if (role != Role.DISTRICT_ADMIN) throw new AccessDeniedException("You are not authorized to perform this action.");
        return actorId;
    }

    private UUID actorId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) throw new AccessDeniedException("You are not authorized to perform this action.");
        try {
            String token = authorization.substring(7);
            String id = jwtService.extractClaim(token, claims -> claims.get("userId", String.class));
            return UUID.fromString(id);
        } catch (Exception e) { throw new AccessDeniedException("You are not authorized to perform this action."); }
    }
}
