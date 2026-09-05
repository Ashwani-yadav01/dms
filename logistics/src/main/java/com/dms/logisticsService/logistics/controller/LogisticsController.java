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
    public ResponseEntity<List<DispatchOrder>> getDispatchesForHospital(@PathVariable UUID hospitalId) {
        return ResponseEntity.ok(dispatchOrderRepository.findByTargetHospitalId(hospitalId));
    }

    // --- SEED / INGESTION ENDPOINTS ---
    @PostMapping("/warehouses")
    public ResponseEntity<Warehouse> createWarehouse(@Valid @RequestBody WarehouseCreateRequest request) {
        return new ResponseEntity<>(managementService.createWarehouse(request), HttpStatus.CREATED);
    }

    @PostMapping("/inventory/batches")
    public ResponseEntity<InventoryBatch> addBatch(@Valid @RequestBody InventoryBatchCreateRequest request) {
        return new ResponseEntity<>(managementService.addInventoryBatch(request), HttpStatus.CREATED);
    }

    @PostMapping("/vehicles")
    public ResponseEntity<TransportVehicle> registerVehicle(@Valid @RequestBody TransportVehicleCreateRequest request) {
        return new ResponseEntity<>(managementService.registerVehicle(request), HttpStatus.CREATED);
    }

    // --- DELIVERY COMPLETION ---
    @PatchMapping("/dispatches/{orderId}/delivered")
    public ResponseEntity<DispatchOrder> markDelivered(@PathVariable UUID orderId) {
        return ResponseEntity.ok(managementService.markOrderAsDelivered(orderId));
    }
}