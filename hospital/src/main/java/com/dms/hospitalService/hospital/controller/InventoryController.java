package com.dms.hospitalService.hospital.controller;

import com.dms.hospitalService.hospital.dto.request.InventoryCreateRequest;
import com.dms.hospitalService.hospital.dto.request.InventoryUpdateRequest;
import com.dms.hospitalService.hospital.dto.response.InventoryResponse;
import com.dms.hospitalService.hospital.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hospitals")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/{hospitalId}/inventory")
    public ResponseEntity<InventoryResponse> initializeInventory(
            @PathVariable UUID hospitalId,
            @Valid @RequestBody InventoryCreateRequest request) {
        return new ResponseEntity<>(inventoryService.initializeInventory(hospitalId, request), HttpStatus.CREATED);
    }

    @PatchMapping("/{hospitalId}/inventory")
    public ResponseEntity<InventoryResponse> updateInventory(
            @PathVariable UUID hospitalId,
            @Valid @RequestBody InventoryUpdateRequest request) {
        return ResponseEntity.ok(inventoryService.updateInventoryQuantity(hospitalId, request));
    }

    @GetMapping("/{hospitalId}/inventory")
    public ResponseEntity<List<InventoryResponse>> getHospitalInventory(@PathVariable UUID hospitalId) {
        return ResponseEntity.ok(inventoryService.getHospitalInventory(hospitalId));
    }

    // AI Forecaster / Supply Chain trigger endpoint
    @GetMapping("/inventory/shortages")
    public ResponseEntity<List<InventoryResponse>> getCriticalShortages() {
        return ResponseEntity.ok(inventoryService.getCriticalShortages());
    }
    @DeleteMapping("/inventory/{id}")
    public ResponseEntity<Void> deleteInventoryItem(@PathVariable UUID id) {
        inventoryService.deleteInventoryItem(id);
        return ResponseEntity.noContent().build();
    }
}