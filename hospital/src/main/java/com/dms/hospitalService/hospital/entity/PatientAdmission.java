package com.dms.hospitalService.hospital.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_patient_admissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientAdmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Ties back to the core incident (e.g., The Fire, The Earthquake)
    @Column(nullable = false)
    private UUID incidentId;

    // Ties back to the Rescue Service that brought them in
    @Column(nullable = true)
    private UUID rescueMissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TriageLevel triageLevel;

    @Column(nullable = false)
    private Boolean requiresIcu;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime admittedAt;

    private LocalDateTime dischargedAt;
}