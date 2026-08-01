package com.dms.userService.user.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CitizenProfileRequest extends UserProfileRequest {
    private String gender;
    private String dateOfBirth;
    private String occupation;
}