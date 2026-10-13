package com.dms.hospitalService.hospital.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "tbl_hospitals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String email;

    private String phoneNumber;

    @Column(length = 500)
    private String address;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FacilityType type;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double elevationMeters; // Crucial for FLOOD routing

    // Bed Capacity
    @Column(nullable = false)
    private Integer totalGeneralBeds;

    @Column(nullable = false)
    private Integer availableGeneralBeds;

    @Column(nullable = false)
    private Integer totalIcuBeds;

    @Column(nullable = false)
    private Integer availableIcuBeds;

    // Capabilities for Smart Routing
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hospital_specialities", joinColumns = @JoinColumn(name = "hospital_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "speciality")
    private Set<MedicalSpeciality> specialities = new HashSet<>();
    // Add these inside Hospital.java
    @OneToMany(mappedBy = "hospital", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicalInventory> inventories = new ArrayList<>();

    @OneToMany(mappedBy = "hospital", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatientAdmission> admissions = new ArrayList<>();
    @Builder.Default
    @Column(nullable = false)
    private Boolean isAcceptingPatients = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private HospitalStatus status = HospitalStatus.ACTIVE;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "admin_user_id")
    private UUID adminUserId;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "location_last_updated_at")
    private LocalDateTime locationLastUpdatedAt;

    @Column(name = "bed_last_updated_at")
    private LocalDateTime bedLastUpdatedAt;

    @Column(name = "emergency_status_last_updated_at")
    private LocalDateTime emergencyStatusLastUpdatedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
