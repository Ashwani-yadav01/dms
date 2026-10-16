package com.dms.hospitalService.hospital.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "tbl_hospital_delivery_receipts",
    indexes = {
        @Index(name = "idx_hdr_hospital", columnList = "hospital_id"),
        @Index(name = "idx_hdr_order", columnList = "dispatch_order_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalDeliveryReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "dispatch_order_id", nullable = false, unique = true)
    private UUID dispatchOrderId;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(name = "source_warehouse_id")
    private UUID sourceWarehouseId;

    @Column(name = "warehouse_name", length = 150)
    private String warehouseName;

    @Column(name = "item_type", nullable = false, length = 60)
    private String itemType;

    @Column(name = "delivered_quantity", nullable = false)
    private Integer deliveredQuantity;

    @Column(name = "vehicle_number", length = 50)
    private String vehicleNumber;

    @Builder.Default
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();
}
