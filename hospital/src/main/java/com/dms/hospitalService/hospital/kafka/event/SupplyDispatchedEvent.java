package com.dms.hospitalService.hospital.kafka.event;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SupplyDispatchedEvent {
    private UUID dispatchOrderId;
    private UUID targetHospitalId;
    private UUID sourceWarehouseId;
    private String warehouseName;
    private String itemType;
    private Integer requestedQuantity;
    private Integer dispatchedQuantity;
    private UUID vehicleId;
    private String vehicleNumber;
    private String status;
    private Instant estimatedArrivalTime;
    private Instant dispatchedAt;
}
