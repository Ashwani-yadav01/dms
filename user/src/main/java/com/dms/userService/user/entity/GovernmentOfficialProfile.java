package com.dms.userService.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "government_official_profiles")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentOfficialProfile extends UserProfile {

    @Column(nullable = false)
    private String departmentName;

    @Column(name = "department_id")
    private UUID departmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DepartmentCategory departmentCategory;

    @Column(nullable = false)
    private String designation;

    @Column(nullable = false, unique = true)
    private String employeeId;

    private String officialPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HierarchyLevel hierarchyLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reports_to")
    @JsonIgnore
    private GovernmentOfficialProfile reportsTo;

    @OneToMany(mappedBy = "reportsTo", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<GovernmentOfficialProfile> subordinates = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfficialStatus status = OfficialStatus.AVAILABLE;

    private Double dutyRadiusKm = 25.0;

    @Column(nullable = false)
    private Boolean isVerified = false;

    private String jurisdictionCode;
}