package com.dms.userService.user.dto.response;

import com.dms.userService.user.entity.DepartmentCategory;
import com.dms.userService.user.entity.HierarchyLevel;
import com.dms.userService.user.entity.OfficialStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class GovernmentOfficialProfileResponse extends UserProfileResponse {

    private UUID id;
    private String departmentName;
    private DepartmentCategory departmentCategory;
    private String designation;
    private String employeeId;
    private String officialPhone;
    private HierarchyLevel hierarchyLevel;
    private OfficialStatus status;
    private Double dutyRadiusKm;
    private Boolean isVerified;
    private String jurisdictionCode;
    private UUID reportsToUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}