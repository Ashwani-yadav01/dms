package com.dms.userService.user.dto.response;

import com.dms.userService.user.entity.Gender;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CitizenProfileResponse extends UserProfileResponse {
    private Gender gender;
    private String dateOfBirth;
    private String occupation;
}