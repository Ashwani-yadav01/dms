package com.dms.userService.user.dto.request;

import com.dms.userService.user.entity.Availability;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class VolunteerProfileRequest extends UserProfileRequest {
    private List<String> skills;
    private Integer experienceInYears;
    private Availability availability;
}