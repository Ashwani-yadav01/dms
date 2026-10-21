package com.dms.userService.user.dto.response;

import com.dms.userService.user.entity.Gender;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CitizenProfileResponse.class, name = "CITIZEN"),
        @JsonSubTypes.Type(value = GovernmentOfficialProfileResponse.class, name = "GOVERNMENT_OFFICIAL"),
        @JsonSubTypes.Type(value = RescueTeamProfileResponse.class, name = "RESCUE_TEAM"),
        @JsonSubTypes.Type(value = DistrictAdminProfileResponse.class, name = "DISTRICT_ADMIN")
})
public abstract class UserProfileResponse {
    private UUID id;
    private String name;
    private String addressLine;
    private String city;
    private String state;
    private String district;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private String profilePhotoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}