package com.dms.logisticsService.logistics.dto.event;

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
public class InventoryShortageAlertEvent {
    private UUID hospitalId;
    private String hospitalName;
    private Double hospitalLatitude;
    private Double hospitalLongitude;
    private ItemType itemType;
    private Integer currentStock;
    private Integer requestedQuantity;
    private String urgencyLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private Integer incomingCasualtyCount;
    private Instant timestamp;
}