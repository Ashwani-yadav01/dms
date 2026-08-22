package com.dms.rescueService.rescue.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationChiefRegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Badge number is required")
    private String badgeNumber;

    @NotBlank(message = "Phone number is required")
    private String phone;
}