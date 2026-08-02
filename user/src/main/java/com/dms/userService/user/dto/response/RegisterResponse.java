package com.dms.userService.user.dto.response;

import com.dms.userService.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    private
    UUID userId;
    private String email;
    private Role role;
    private boolean profileCompleted;
    private String message;
}