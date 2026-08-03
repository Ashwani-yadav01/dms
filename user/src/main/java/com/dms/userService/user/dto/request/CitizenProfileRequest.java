package com.dms.userService.user.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class CitizenProfileRequest extends UserProfileRequest {
    private String gender;
    private LocalDate dateOfBirth;
    private String occupation;
}