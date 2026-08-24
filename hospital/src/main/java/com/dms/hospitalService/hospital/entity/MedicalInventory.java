package com.dms.hospitalService.hospital.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_medical_inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryItemType itemType;

    @Column(nullable = false)
    private Integer currentQuantity;

    // When currentQuantity drops below this, we trigger Kafka alerts for redistribution
    @Column(nullable = false)
    private Integer criticalThreshold;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;
}