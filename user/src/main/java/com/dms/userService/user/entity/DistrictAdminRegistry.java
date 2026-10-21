package com.dms.userService.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "district_admin_registry", indexes = @Index(name = "idx_district_admin_registry_employee", columnList = "employee_id", unique = true))
@Getter
@Setter
@NoArgsConstructor
public class DistrictAdminRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false, unique = true)
    private String employeeId;

    @Column(name = "authorization_code", nullable = false, unique = true)
    private String authorizationCode;

    @Column(name = "admin_email")
    private String adminEmail;

    @Column(name = "admin_phone", length = 20)
    private String adminPhone;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "department_category", length = 40)
    private String departmentCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DistrictAdminRegistryStatus status = DistrictAdminRegistryStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public boolean isActive() {
        return status == DistrictAdminRegistryStatus.ACTIVE;
    }
}
