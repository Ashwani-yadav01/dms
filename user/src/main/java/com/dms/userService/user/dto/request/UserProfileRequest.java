package com.dms.userService.user.dto.request;

import com.dms.userService.user.entity.Gender;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private Double latitude;
    @NotNull
    private Double longitude;
    private String profilePhotoUrl;
}