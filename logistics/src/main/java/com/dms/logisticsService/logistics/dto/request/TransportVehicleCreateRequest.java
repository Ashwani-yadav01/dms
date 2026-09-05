package com.dms.logisticsService.logistics.dto.request;

import com.dms.logisticsService.logistics.entity.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record TransportVehicleCreateRequest(
        @NotBlank String vehicleNumber,
        @NotNull VehicleType type,
        @NotNull @Positive Double maxPayloadKg,
        @NotNull UUID baseWarehouseId
) {}