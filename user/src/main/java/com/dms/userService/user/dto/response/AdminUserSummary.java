package com.dms.userService.user.dto.response;

import com.dms.userService.user.entity.Role;

import java.time.Instant;
import java.util.UUID;

public record AdminUserSummary(
        UUID id,
        String email,
        String mobileNumber,
        Role role,
        boolean profileCompleted,
        Instant createdAt,
        String employeeId,
        String verificationCode
) {}
