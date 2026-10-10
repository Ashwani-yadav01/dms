package com.dms.userService.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body used by the Super Admin to issue a hospital invitation.
 * The employee ID and verification code are generated server-side and shared
 * with the prospective hospital administrator.
 */
@Data
public class CreateHospitalInvitationRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
    private String mobileNumber;

    private String hospitalName;
}