package com.dms.userService.user.dto.request;

import com.dms.userService.user.entity.Gender;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
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
        @JsonSubTypes.Type(value = GovernmentOfficialProfileRequest.class, name = "GOVERNMENT_OFFICIAL"),
        @JsonSubTypes.Type(value = RescueTeamProfileRequest.class, name = "RESCUE_TEAM")
})
@Data
public abstract class UserProfileRequest {

    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "addressLine is required")
    private String addressLine;
    @NotBlank(message = "city is required")
    private String city;
    @NotBlank(message = "State is required")
    private String state;
    @NotBlank(message = "district is required")
    private String district;
    @NotBlank(message = "Pincode is required")
    private String pincode;
    @NotNull
    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private Double latitude;
    @NotNull
    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double longitude;
    private String profilePhotoUrl;
}
