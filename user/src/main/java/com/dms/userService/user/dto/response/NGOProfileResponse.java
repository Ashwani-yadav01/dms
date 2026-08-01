package com.dms.userService.user.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NGOProfileResponse extends UserProfileResponse {
    private String ngoName;
    private String registrationNumber;
    private String ownerName;
    private String website;
    private String description;
}