package com.dms.userService.user.dto.response;

import com.dms.userService.user.entity.Availability;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class VolunteerProfileResponse extends UserProfileResponse {

    private List<String> skills;
    private Integer experienceInYears;
    private Availability availability;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}