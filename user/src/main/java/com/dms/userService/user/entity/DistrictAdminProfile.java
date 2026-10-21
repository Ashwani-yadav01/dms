package com.dms.userService.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "district_admin_profiles")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DistrictAdminProfile extends UserProfile {

    private String districtName;

    private String jurisdiction;

    private String designation;

    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "department_category", length = 30)
    private DepartmentCategory departmentCategory;

    private String controlRoomHelpline;

    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "helpline_number")
    private String helplineNumber;

    public String getHelplineNumber() {
        return helplineNumber != null ? helplineNumber : controlRoomHelpline;
    }
}
