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

    // Ties back to the core incident (e.g., The Fire, The Earthquake) - optional for walk-ins/mass casualties
    @Column(nullable = true)
    private UUID incidentId;

    // Ties back to the Rescue Service that brought them in
    @Column(nullable = true)
    private UUID rescueMissionId;

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(name = "national_id", length = 50)
    private String nationalId;

    @Column(name = "incident_cause", length = 255)
    private String incidentCause;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TriageLevel triageLevel;

    @Column(nullable = false)
    private Boolean requiresIcu;

    @Column(name = "patient_name", length = 150)
    private String patientName;

    private Integer age;

    @Column(length = 20)
    private String gender;

    @Column(name = "triage_notes", columnDefinition = "TEXT")
    private String triageNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AdmissionStatus status = AdmissionStatus.ADMITTED;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime admittedAt;

    private LocalDateTime dischargedAt;
}