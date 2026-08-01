package com.dms.userService.user.dto.response;

import com.dms.userService.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String mobileNumber;
    private Role role;
    private UserProfileResponse userProfile;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}