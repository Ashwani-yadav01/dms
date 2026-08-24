package com.dms.hospitalService.hospital.dto.response;

import com.dms.hospitalService.hospital.entity.InventoryItemType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InventoryResponse {
    private UUID id;
    private UUID hospitalId;
    private InventoryItemType itemType;
    private Integer currentQuantity;
    private Integer criticalThreshold;
    private Boolean isCriticalShortage; // We will calculate this in the service layer
    private LocalDateTime lastUpdated;
}