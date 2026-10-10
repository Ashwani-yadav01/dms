package com.dms.userService.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Hospital invitation registry. A Super Admin creates an entry here before any
 * hospital user can register. The employee ID and verification code are shared
 * with the prospective hospital administrator, who must supply both during
 * registration. This mirrors the GovernmentRegistry pattern used for
 * government officials.
 */
@Entity
@Table(name = "hospital_registry", indexes = @Index(name = "idx_hospital_registry_employee", columnList = "employee_id", unique = true))
@Getter @Setter @NoArgsConstructor
public class HospitalRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hospital_profile_id", length = 32)
    private String hospitalProfileId;

    @Column(name = "employee_id", nullable = false, unique = true)
    private String employeeId;

    @Column(name = "hospital_name")
    private String hospitalName;

    @Column(name = "authorization_code", nullable = false, unique = true)
    private String authorizationCode;

    @Column(name = "hospital_email")
    private String hospitalEmail;

    @Column(name = "hospital_phone", length = 20)
    private String hospitalPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private HospitalRegistryStatus status = HospitalRegistryStatus.ACTIVE;

    @Column(name = "created_by")
    private String createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public boolean isActive() {
        return status == HospitalRegistryStatus.ACTIVE;
    }
}