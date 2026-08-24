package com.dms.hospitalService.hospital.dto.request;

import com.dms.hospitalService.hospital.entity.InventoryItemType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryCreateRequest {

    @NotNull(message = "Item type is required")
    private InventoryItemType itemType;

    @NotNull(message = "Initial quantity is required")
    @Min(value = 0, message = "Initial quantity cannot be negative")
    private Integer initialQuantity;

    @NotNull(message = "Critical threshold is required")
    @Min(value = 0, message = "Threshold cannot be negative")
    private Integer criticalThreshold;
}