package com.dms.userService.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NGOProfileRequest extends UserProfileRequest {
    @NotBlank(message = "NGO Name is required")
    private String ngoName;

    @NotBlank(message = "Registration Number is required")
    private String registrationNumber;

    @NotBlank(message = "Owner Name is required")
    private String ownerName;

    private String website;

    private String description;
}