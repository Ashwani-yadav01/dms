package com.dms.userService.user.dto.request;

import com.dms.userService.user.entity.DepartmentCategory;
import com.dms.userService.user.entity.HierarchyLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GovernmentOfficialProfileRequest extends UserProfileRequest {

    @NotBlank(message = "Department name is required")
    private String departmentName;

    // Optional foreign reference to physical Rescue Department in Rescue Service
    private UUID departmentId;

    @NotNull(message = "Department category is required")
    private DepartmentCategory departmentCategory;

    private String designation;

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    private String officialPhone;

    private HierarchyLevel hierarchyLevel = HierarchyLevel.LOCAL_OFFICER;

    private Double dutyRadiusKm = 25.0;

    private String jurisdictionCode;

    private UUID reportsToUserId; // Optional supervisor

    @NotBlank(message = "Authorization code is required")
    private String authorizationCode;

    @JsonSetter("departmentId")
    public void setDepartmentId(Object val) {
        if (val == null) {
            this.departmentId = null;
        } else if (val instanceof UUID uuid) {
            this.departmentId = uuid;
        } else if (val instanceof String s && !s.trim().isEmpty()) {
            try {
                this.departmentId = UUID.fromString(s.trim());
            } catch (IllegalArgumentException e) {
                this.departmentId = null;
            }
        } else {
            this.departmentId = null;
        }
    }

    public String getDesignation() {
        if (designation != null && !designation.trim().isEmpty()) {
            return designation.trim();
        }
        return (departmentName != null && !departmentName.trim().isEmpty())
                ? departmentName + " Officer"
                : "Station Officer";
    }

    public HierarchyLevel getHierarchyLevel() {
        return hierarchyLevel != null ? hierarchyLevel : HierarchyLevel.LOCAL_OFFICER;
    }

    public Double getDutyRadiusKm() {
        return (dutyRadiusKm != null && dutyRadiusKm > 0) ? dutyRadiusKm : 25.0;
    }

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

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId == null ? null : employeeId.trim().toUpperCase();
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode == null ? null : authorizationCode.trim().toUpperCase();
    }
}
