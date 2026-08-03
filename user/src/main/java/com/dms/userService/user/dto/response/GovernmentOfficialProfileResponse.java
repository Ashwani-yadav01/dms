package com.dms.userService.user.dto.response;

import com.dms.userService.user.entity.HierarchyLevel;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GovernmentOfficialProfileResponse extends UserProfileResponse {

    private GovernmentDepartmentResponse department;
    private String designation;
    private String employeeId;
    private String officialPhone;
    private HierarchyLevel hierarchyLevel;
    private UUID reportsToOfficialId;
}