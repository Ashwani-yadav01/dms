package com.dms.userService.user.dto.response;

import com.dms.userService.user.entity.Gender;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class CitizenProfileResponse extends UserProfileResponse {
    private Gender gender;
    private LocalDate dateOfBirth;
    private String occupation;
}