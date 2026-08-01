package com.dms.userService.user.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CitizenProfileResponse extends UserProfileResponse {
    private String gender;
    private String dateOfBirth;
    private String occupation;
}