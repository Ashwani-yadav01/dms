package com.dms.userService.user.entity;

import com.dms.userService.user.dto.request.GovernmentOfficialProfileRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "government_registry", indexes = @Index(name = "idx_gov_registry_employee", columnList = "employee_id", unique = true))
@Getter @Setter @NoArgsConstructor
public class GovernmentRegistry {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "government_profile_id", length = 32)
    private String governmentProfileId;
    @Column(name = "employee_id", nullable = false, unique = true)
    private String employeeId;
    @Column(name = "department_name")
    private String departmentName;
    @Column
    private String designation;
    @Enumerated(EnumType.STRING) @Column(name = "hierarchy_level", length = 30)
    private HierarchyLevel hierarchyLevel;
    @Column(name = "authorization_code", nullable = false, unique = true)
    private String authorizationCode;
    @Column(name = "official_email")
    private String officialEmail;
    @Column(name = "official_phone", length = 20)
    private String officialPhone;
    @Column(name = "office_latitude")
    private Double officeLatitude;
    @Column(name = "office_longitude")
    private Double officeLongitude;
    @Column(name = "duty_radius_km", nullable = false)
    private Double dutyRadiusKm = 20.0;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private GovernmentRegistryStatus status = GovernmentRegistryStatus.ACTIVE;
    @CreationTimestamp @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(nullable = false)
    private Instant updatedAt;

    public boolean matches(GovernmentOfficialProfileRequest request) {
        return equal(employeeId, request.getEmployeeId())
                && optionalEqual(departmentName, request.getDepartmentName())
                && optionalEqual(designation, request.getDesignation())
                && (hierarchyLevel == null || hierarchyLevel == request.getHierarchyLevel())
                && equal(authorizationCode, request.getAuthorizationCode());
    }
    private boolean equal(String a, String b) { return a != null && b != null && a.trim().equalsIgnoreCase(b.trim()); }
    private boolean optionalEqual(String stored, String supplied) { return stored == null || equal(stored, supplied); }
}
