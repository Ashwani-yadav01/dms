package com.dms.userService.user.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class VolunteerProfileRequest extends UserProfileRequest {

    @NotEmpty(message = "At least one skill is required")
    private List<String> skills;

    private Integer experienceInYears;

    @NotNull(message = "Availability status is required")
    private String availability;
}