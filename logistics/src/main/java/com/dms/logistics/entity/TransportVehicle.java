package com.dms.logistics.entity;

import com.dms.logistics.entity.enums.VehicleStatus;
import com.dms.logistics.entity.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transport_vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vehicle_number", nullable = false, unique = true, length = 30)
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VehicleType type;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 30)
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @Column(name = "max_payload_kg", nullable = false)
    private Double maxPayloadKg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_warehouse_id")
    private Warehouse baseWarehouse;

    private Double currentLatitude;
    private Double currentLongitude;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}