package com.dms.userService.user.dto.request;

import com.dms.userService.user.entity.DepartmentCategory;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DistrictAdminProfileRequest extends UserProfileRequest {

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
