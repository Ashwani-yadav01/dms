package com.dms.hospitalService.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_hospital_audits", indexes = @Index(name = "idx_hospital_audit_hospital", columnList = "hospital_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(name = "performed_by")
    private UUID performedBy;

    @Column(name = "actor_role", length = 32)
    private String actorRole;

    @Column(nullable = false, length = 64)
    private String action; // CREATE, VERIFY, REJECT, SUSPEND, RESUME, BED_UPDATE, LOCATION_UPDATE, INVENTORY_BATCH

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "previous_value", length = 255)
    private String previousValue;

    @Column(name = "new_value", length = 255)
    private String newValue;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();
}
