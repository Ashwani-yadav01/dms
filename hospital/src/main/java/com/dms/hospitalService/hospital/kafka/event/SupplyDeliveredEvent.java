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
public class SupplyDeliveredEvent {
    private UUID dispatchOrderId;
    private UUID targetHospitalId;
    private UUID sourceWarehouseId;
    private String warehouseName;
    private String itemType;
    private Integer requestedQuantity;
    private Integer deliveredQuantity;
    private UUID vehicleId;
    private String vehicleNumber;
    private String status;
    private Instant deliveredAt;
}
