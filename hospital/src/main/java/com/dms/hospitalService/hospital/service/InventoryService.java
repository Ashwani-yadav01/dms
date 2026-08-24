package com.dms.hospitalService.hospital.service;

import com.dms.hospitalService.hospital.dto.request.InventoryCreateRequest;
import com.dms.hospitalService.hospital.dto.request.InventoryUpdateRequest;
import com.dms.hospitalService.hospital.dto.response.InventoryResponse;

import java.util.List;
import java.util.UUID;

public interface InventoryService {
    InventoryResponse initializeInventory(UUID hospitalId, InventoryCreateRequest request);

    // Updates inventory. Uses pessimistic locking to handle concurrent updates safely.
    InventoryResponse updateInventoryQuantity(UUID hospitalId, InventoryUpdateRequest request);

    List<InventoryResponse> getHospitalInventory(UUID hospitalId);

    // Used by the AI/Supply chain forecaster to find nationwide shortages
    List<InventoryResponse> getCriticalShortages();

    void deleteInventoryItem(UUID id);
}