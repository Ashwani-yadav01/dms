package com.dms.logisticsService.logistics.controller;

import com.dms.logisticsService.logistics.entity.DispatchOrder;
import com.dms.logisticsService.logistics.entity.Warehouse;
import com.dms.logisticsService.logistics.repository.DispatchOrderRepository;
import com.dms.logisticsService.logistics.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/warehouses")
    public ResponseEntity<List<Warehouse>> getAllWarehouses() {
        return ResponseEntity.ok(warehouseRepository.findAll());
    }

    @GetMapping("/dispatches/hospital/{hospitalId}")
    public ResponseEntity<List<DispatchOrder>> getDispatchesForHospital(@PathVariable UUID hospitalId) {
        return ResponseEntity.ok(dispatchOrderRepository.findByTargetHospitalId(hospitalId));
    }
}