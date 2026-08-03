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
        @JsonSubTypes.Type(value = VolunteerProfileResponse.class, name = "VOLUNTEER"),
        @JsonSubTypes.Type(value = NGOProfileResponse.class, name = "NGO"),
        @JsonSubTypes.Type(value = GovernmentOfficialProfileResponse.class, name = "GOVERNMENT_OFFICIAL")
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