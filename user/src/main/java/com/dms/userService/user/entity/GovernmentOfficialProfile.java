package com.dms.userService.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "government_official_profiles")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyJoinColumn(name = "user_id")
public class GovernmentOfficialProfile extends UserProfile {

//    @Id
//    private UUID id;
//
//    @OneToOne(fetch = FetchType.LAZY, optional = false)
//    @MapsId
//    @JoinColumn(name = "user_id", nullable = false, unique = true)
//    private User user;

    // --- EMBEDDED DEPARTMENT DETAILS ---
    @Column(nullable = false)
    private String departmentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepartmentCategory departmentCategory;

    // --- OFFICIAL IDENTIFICATION & POSITION ---
    @Column(nullable = false)
    private String designation;

    @Column(nullable = false, unique = true)
    private String employeeId;

    private String officialPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HierarchyLevel hierarchyLevel;

    // --- HIERARCHY / REPORTING ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reports_to")
    private GovernmentOfficialProfile reportsTo;

    @OneToMany(mappedBy = "reportsTo", fetch = FetchType.LAZY)
    private List<GovernmentOfficialProfile> subordinates = new ArrayList<>();

    // --- ALLOCATION ENGINE FIELDS ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfficialStatus status = OfficialStatus.AVAILABLE;

    private Double dutyRadiusKm = 25.0; // Max distance in KM official can respond from base coordinates

    @Column(nullable = false)
    private Boolean isVerified = false; // Verified flag for emergency assignments

    private String jurisdictionCode; // e.g. "DISTRICT_DEHRADUN_ZONE_1"

    // --- AUDITING ---
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}