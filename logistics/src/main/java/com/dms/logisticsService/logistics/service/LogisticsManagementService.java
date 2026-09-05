package com.dms.logisticsService.logistics.service;

import com.dms.logisticsService.logistics.dto.request.InventoryBatchCreateRequest;
import com.dms.logisticsService.logistics.dto.request.TransportVehicleCreateRequest;
import com.dms.logisticsService.logistics.dto.request.WarehouseCreateRequest;
import com.dms.logisticsService.logistics.entity.DispatchOrder;
import com.dms.logisticsService.logistics.entity.InventoryBatch;
import com.dms.logisticsService.logistics.entity.TransportVehicle;
import com.dms.logisticsService.logistics.entity.Warehouse;
import com.dms.logisticsService.logistics.entity.enums.DispatchStatus;
import com.dms.logisticsService.logistics.entity.enums.TemperatureClass;
import com.dms.logisticsService.logistics.entity.enums.VehicleStatus;
import com.dms.logisticsService.logistics.repository.DispatchOrderRepository;
import com.dms.logisticsService.logistics.repository.InventoryBatchRepository;
import com.dms.logisticsService.logistics.repository.TransportVehicleRepository;
import com.dms.logisticsService.logistics.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsManagementService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final TransportVehicleRepository transportVehicleRepository;
    private final DispatchOrderRepository dispatchOrderRepository;

    @Transactional
    public Warehouse createWarehouse(WarehouseCreateRequest request) {
        Warehouse warehouse = Warehouse.builder()
                .name(request.name())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .address(request.address())
                .isActive(true)
                .build();
        return warehouseRepository.save(warehouse);
    }

    @Transactional
    public InventoryBatch addInventoryBatch(InventoryBatchCreateRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + request.warehouseId()));

        InventoryBatch batch = InventoryBatch.builder()
                .warehouse(warehouse)
                .itemType(request.itemType())
                .batchNumber(request.batchNumber())
                .quantity(request.quantity())
                .reservedQuantity(0)
                .expiryDate(request.expiryDate())
                .temperatureClass(request.temperatureClass() != null ? request.temperatureClass() : TemperatureClass.ROOM_TEMP)
                .build();

        return inventoryBatchRepository.save(batch);
    }

    @Transactional
    public TransportVehicle registerVehicle(TransportVehicleCreateRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.baseWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + request.baseWarehouseId()));

        TransportVehicle vehicle = TransportVehicle.builder()
                .vehicleNumber(request.vehicleNumber())
                .type(request.type())
                .maxPayloadKg(request.maxPayloadKg())
                .status(VehicleStatus.AVAILABLE)
                .baseWarehouse(warehouse)
                .currentLatitude(warehouse.getLatitude())
                .currentLongitude(warehouse.getLongitude())
                .build();

        return transportVehicleRepository.save(vehicle);
    }

    /**
     * Completes the delivery: marks the order DELIVERED and releases the vehicle back to AVAILABLE.
     */
    @Transactional
    public DispatchOrder markOrderAsDelivered(UUID orderId) {
        DispatchOrder order = dispatchOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Dispatch order not found: " + orderId));

        if (order.getStatus() == DispatchStatus.DELIVERED) {
            log.warn("Order {} is already delivered.", orderId);
            return order;
        }

        order.setStatus(DispatchStatus.DELIVERED);

        // Free up the vehicle
        TransportVehicle vehicle = order.getAssignedVehicle();
        if (vehicle != null) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            transportVehicleRepository.save(vehicle);
            log.info("Vehicle {} has returned to AVAILABLE status.", vehicle.getVehicleNumber());
        }

        DispatchOrder updatedOrder = dispatchOrderRepository.save(order);
        log.info("Dispatch order {} updated to DELIVERED.", orderId);
        return updatedOrder;
    }
}