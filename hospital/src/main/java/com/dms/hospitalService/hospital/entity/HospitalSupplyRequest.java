package com.dms.hospitalService.hospital.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "tbl_hospital_supply_requests",
    indexes = {
        @Index(name = "idx_hsr_hospital", columnList = "hospital_id"),
        @Index(name = "idx_hsr_status", columnList = "status"),
        @Index(name = "idx_hsr_urgency", columnList = "urgency_level")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalSupplyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(name = "hospital_name", length = 150)
    private String hospitalName;

    @Column(name = "item_type", nullable = false, length = 60)
    private String itemType;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Builder.Default
    @Column(name = "urgency_level", nullable = false, length = 30)
    private String urgencyLevel = "CRITICAL";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SupplyRequestStatus status = SupplyRequestStatus.PENDING;

    @Column(name = "admin_notes", length = 500)
    private String adminNotes;

    @Column(name = "actioned_by_admin_id")
    private UUID actionedByAdminId;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "actioned_at")
    private Instant actionedAt;
}
