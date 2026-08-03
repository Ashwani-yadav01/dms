package com.dms.userService.user.dto.request;

import com.dms.userService.user.entity.HierarchyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GovernmentOfficialProfileRequest extends UserProfileRequest {

    @NotNull(message = "Department ID is required")
    private UUID departmentId;

    @NotBlank(message = "Designation is required")
    private String designation;

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    private String officialPhone;

    @NotNull(message = "Hierarchy Level is required")
    private HierarchyLevel hierarchyLevel;

    private UUID reportsToOfficialId;
}