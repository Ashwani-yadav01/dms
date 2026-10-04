package com.dms.userService.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateGovernmentOfficialRequest {
    @NotBlank @Email
    private String email;
    @NotBlank @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
    private String mobileNumber;
}
