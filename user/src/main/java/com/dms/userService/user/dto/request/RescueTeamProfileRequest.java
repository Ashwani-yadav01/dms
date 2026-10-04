package com.dms.userService.user.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.UUID;

@Data @EqualsAndHashCode(callSuper = true)
public class RescueTeamProfileRequest extends UserProfileRequest {
    @NotBlank private String teamName;
    @NotBlank private String teamCode;
    @NotNull private UUID departmentId;
    @NotBlank private String teamLeadName;
    @NotNull @Min(1) private Integer teamSize;
}
