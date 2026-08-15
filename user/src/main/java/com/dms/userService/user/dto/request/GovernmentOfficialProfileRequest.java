package com.dms.userService.user.dto.request;

import com.dms.userService.user.entity.DepartmentCategory;
import com.dms.userService.user.entity.HierarchyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class GovernmentOfficialProfileRequest extends UserProfileRequest {

    @NotBlank(message = "Department name is required")
    private String departmentName;

    // Optional foreign reference to physical Rescue Department in Rescue Service
    private UUID departmentId;

    @NotNull(message = "Department category is required")
    private DepartmentCategory departmentCategory;

    @NotBlank(message = "Designation is required")
    private String designation;

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    private String officialPhone;

    @NotNull(message = "Hierarchy level is required")
    private HierarchyLevel hierarchyLevel;

    private Double dutyRadiusKm = 25.0;

    private String jurisdictionCode;

    private UUID reportsToUserId; // Optional supervisor

    // --- NORMALIZE INPUT STRING TO PREVENT INCONSISTENCY ---
    public void setDepartmentName(String departmentName) {
        if (departmentName != null) {
            this.departmentName = departmentName.trim()
                    .replaceAll("\\s+", " ")
                    .replace("&", "AND")
                    .toUpperCase();
        } else {
            this.departmentName = null;
        }
    }
}