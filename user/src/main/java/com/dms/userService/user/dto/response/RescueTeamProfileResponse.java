package com.dms.userService.user.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.UUID;

@Data @EqualsAndHashCode(callSuper = true)
public class RescueTeamProfileResponse extends UserProfileResponse {
    private UUID id;
    private String teamName;
    private String teamCode;
    private UUID departmentId;
    private String teamLeadName;
    private Integer teamSize;
    private Boolean isVerified;
}
