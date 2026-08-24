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

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}