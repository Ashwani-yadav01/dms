package com.dms.userService.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentDepartmentRequest {

    @NotBlank(message = "Department name is required")
    private String departmentName;

    private String description;
}