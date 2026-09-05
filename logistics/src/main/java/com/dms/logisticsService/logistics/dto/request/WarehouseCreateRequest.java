package com.dms.logisticsService.logistics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WarehouseCreateRequest(
        @NotBlank String name,
        @NotNull Double latitude,
        @NotNull Double longitude,
        String address
) {}