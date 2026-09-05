package com.dms.logisticsService.logistics.dto.request;

import com.dms.logisticsService.logistics.entity.enums.ItemType;
import com.dms.logisticsService.logistics.entity.enums.TemperatureClass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record InventoryBatchCreateRequest(
        @NotNull UUID warehouseId,
        @NotNull ItemType itemType,
        @NotBlank String batchNumber,
        @NotNull @Positive Integer quantity,
        @NotNull LocalDate expiryDate,
        TemperatureClass temperatureClass
) {}