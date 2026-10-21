package com.dms.hospitalService.hospital.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_district_medical_reserves")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistrictMedicalReserve {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private InventoryItemType itemType;

    @Column(nullable = false)
    private Integer currentQuantity;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;
}
