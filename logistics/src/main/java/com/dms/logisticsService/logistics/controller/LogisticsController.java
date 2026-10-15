package com.dms.logisticsService.logistics.controller;

import com.dms.logisticsService.logistics.dto.request.InventoryBatchCreateRequest;
import com.dms.logisticsService.logistics.dto.request.TransportVehicleCreateRequest;
import com.dms.logisticsService.logistics.dto.request.WarehouseCreateRequest;
import com.dms.logisticsService.logistics.entity.DispatchOrder;
import com.dms.logisticsService.logistics.entity.InventoryBatch;
import com.dms.logisticsService.logistics.entity.TransportVehicle;
import com.dms.logisticsService.logistics.entity.Warehouse;
import com.dms.logisticsService.logistics.repository.DispatchOrderRepository;
import com.dms.logisticsService.logistics.repository.WarehouseRepository;
import com.dms.logisticsService.logistics.service.LogisticsManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final WarehouseRepository warehouseRepository;
    private final DispatchOrderRepository dispatchOrderRepository;
    private final LogisticsManagementService managementService;

    // --- READ ENDPOINTS ---
    @GetMapping("/warehouses")
    public ResponseEntity<List<Warehouse>> getAllWarehouses() {
        return ResponseEntity.ok(warehouseRepository.findAll());
    }

    @GetMapping("/dispatches/hospital/{hospitalId}")
    public ResponseEntity<List<DispatchOrder>> getDispatchesForHospital(
            @PathVariable UUID hospitalId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!canViewHospitalDispatches(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(dispatchOrderRepository.findByTargetHospitalId(hospitalId));
    }

    // --- SEED / INGESTION ENDPOINTS ---
    @PostMapping("/warehouses")
    public ResponseEntity<Warehouse> createWarehouse(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody WarehouseCreateRequest request) {
        if (!canManageLogistics(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return new ResponseEntity<>(managementService.createWarehouse(request), HttpStatus.CREATED);
    }

    @PostMapping("/inventory/batches")
    public ResponseEntity<InventoryBatch> addBatch(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody InventoryBatchCreateRequest request) {
        if (!canManageLogistics(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return new ResponseEntity<>(managementService.addInventoryBatch(request), HttpStatus.CREATED);
    }

    @PostMapping("/vehicles")
    public ResponseEntity<TransportVehicle> registerVehicle(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody TransportVehicleCreateRequest request) {
        if (!canManageLogistics(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return new ResponseEntity<>(managementService.registerVehicle(request), HttpStatus.CREATED);
    }

    // --- DELIVERY COMPLETION ---
    @PatchMapping("/dispatches/{orderId}/delivered")
    public ResponseEntity<DispatchOrder> markDelivered(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable UUID orderId) {
        if (!canManageLogistics(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(managementService.markOrderAsDelivered(orderId));
    }

    private boolean canManageLogistics(String role) {
        return "DISTRICT_ADMIN".equals(role)
                || "SUPER_ADMIN".equals(role)
                || "GOVERNMENT_OFFICIAL".equals(role)
                || "NGO".equals(role);
    }

    private boolean canViewHospitalDispatches(String role) {
        return canManageLogistics(role) || "HOSPITAL".equals(role);
    }
}