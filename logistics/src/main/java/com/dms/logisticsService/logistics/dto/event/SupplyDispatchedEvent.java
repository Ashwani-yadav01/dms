package com.dms.logisticsService.logistics.dto.event;

import com.dms.logisticsService.logistics.entity.enums.DispatchStatus;
import com.dms.logisticsService.logistics.entity.enums.ItemType;
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
    private ItemType itemType;
    private Integer requestedQuantity;
    private Integer dispatchedQuantity;
    private UUID vehicleId;
    private String vehicleNumber;
    private DispatchStatus status;
    private Instant estimatedArrivalTime;
    private Instant dispatchedAt;
}