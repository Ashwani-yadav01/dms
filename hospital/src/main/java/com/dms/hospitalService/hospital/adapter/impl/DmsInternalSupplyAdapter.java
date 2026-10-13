package com.dms.hospitalService.hospital.adapter.impl;

import com.dms.hospitalService.hospital.adapter.ExternalSupplySyncAdapter;
import com.dms.hospitalService.hospital.dto.response.InventoryResponse;
import com.dms.hospitalService.hospital.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Default internal DMS supply adapter.
 */
@Component
public class DmsInternalSupplyAdapter implements ExternalSupplySyncAdapter {

    private final InventoryService inventoryService;

    public DmsInternalSupplyAdapter(@Lazy InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public String getSourceSystemName() {
        return "DMS_INTERNAL_INVENTORY";
    }

    @Override
    public List<InventoryResponse> syncSuppliesFromSource(UUID hospitalId) {
        return inventoryService.getHospitalInventory(hospitalId);
    }
}
