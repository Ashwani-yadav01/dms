package com.dms.userService.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentDepartmentRequest {

    @NotBlank(message = "Department name is required")
    private String departmentName;

    private String description;
}