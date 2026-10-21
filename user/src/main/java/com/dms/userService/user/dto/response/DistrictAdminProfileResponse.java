package com.dms.userService.user.dto.response;

import com.dms.userService.user.entity.DepartmentCategory;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class DistrictAdminProfileResponse extends UserProfileResponse {

    private UUID id;

    private String employeeId;

    private String verificationCode;

    private String helplineNumber;

    private String districtName;

    private String jurisdiction;

    private String designation;

    private String department;

    private DepartmentCategory departmentCategory;

    private String controlRoomHelpline;

    public String getHelplineNumber() {
        return helplineNumber != null ? helplineNumber : controlRoomHelpline;
    }
}
