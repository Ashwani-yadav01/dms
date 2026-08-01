package com.dms.userService.user.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CitizenProfileRequest.class, name = "CITIZEN"),
        @JsonSubTypes.Type(value = VolunteerProfileRequest.class, name = "VOLUNTEER"),
        @JsonSubTypes.Type(value = NGOProfileRequest.class, name = "NGO"),
        @JsonSubTypes.Type(value = GovernmentOfficialProfileRequest.class, name = "GOVERNMENT_OFFICIAL")
})
@Getter
@Setter
@Data
public abstract class UserProfileRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String addressLine;
    private String city;
    private String state;
    private String district;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private String profilePhotoUrl;
}