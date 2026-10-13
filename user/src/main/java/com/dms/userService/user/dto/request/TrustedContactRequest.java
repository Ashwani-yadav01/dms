package com.dms.userService.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustedContactRequest {

    @NotBlank(message = "Contact name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 50, message = "Relationship must not exceed 50 characters")
    private String relationship;

    private String phone;

    private String email;

    private boolean isEmergencyContact;
}
