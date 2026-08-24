package com.dms.hospitalService.hospital.dto.request;

import com.dms.hospitalService.hospital.entity.InventoryItemType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryUpdateRequest {

    @NotNull(message = "Item type is required")
    private InventoryItemType itemType;

    @NotNull(message = "Quantity change is required (can be negative for consumption)")
    private Integer quantityChange;

    // Optional: Only used when first initializing or changing the threshold
    private Integer criticalThreshold;
}