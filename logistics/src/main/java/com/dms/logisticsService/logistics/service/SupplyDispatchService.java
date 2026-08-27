package com.dms.logisticsService.logistics.service;

import com.dms.logisticsService.logistics.dto.event.InventoryShortageAlertEvent;
import com.dms.logisticsService.logistics.dto.event.SupplyDispatchedEvent;
import com.dms.logisticsService.logistics.entity.DispatchOrder;
import com.dms.logisticsService.logistics.entity.InventoryBatch;
import com.dms.logisticsService.logistics.entity.TransportVehicle;
import com.dms.logisticsService.logistics.entity.Warehouse;
import com.dms.logisticsService.logistics.entity.enums.DispatchStatus;
import com.dms.logisticsService.logistics.entity.enums.VehicleStatus;
import com.dms.logisticsService.logistics.repository.DispatchOrderRepository;
import com.dms.logisticsService.logistics.repository.InventoryBatchRepository;
import com.dms.logisticsService.logistics.repository.TransportVehicleRepository;
import com.dms.logisticsService.logistics.repository.WarehouseRepository;
import com.dms.logisticsService.logistics.util.GeoDistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplyDispatchService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final TransportVehicleRepository transportVehicleRepository;
    private final DispatchOrderRepository dispatchOrderRepository;
    private final GeoDistanceCalculator distanceCalculator;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String SUPPLY_DISPATCHED_TOPIC = "supply-dispatched-topic";
    private static final double AVERAGE_TRANSIT_SPEED_KMH = 45.0; // Average emergency vehicle speed in disaster zones

    @Transactional
    public Optional<DispatchOrder> processShortageAlert(InventoryShortageAlertEvent event) {
        log.info("Processing supply shortage alert for Hospital: {} ({}), Item: {}, Requested: {}",
                event.getHospitalName(), event.getHospitalId(), event.getItemType(), event.getRequestedQuantity());

        // 1. Find all active warehouses and rank by distance to hospital
        List<Warehouse> activeWarehouses = warehouseRepository.findByIsActiveTrue();
        if (activeWarehouses.isEmpty()) {
            log.error("No active warehouses available in the system!");
            return Optional.empty();
        }

        activeWarehouses.sort(Comparator.comparingDouble(w ->
                distanceCalculator.calculateDistanceKm(
                        event.getHospitalLatitude(), event.getHospitalLongitude(),
                        w.getLatitude(), w.getLongitude()
                )
        ));

        // 2. Iterate through closest warehouses and attempt FEFO allocation
        for (Warehouse warehouse : activeWarehouses) {
            double distanceKm = distanceCalculator.calculateDistanceKm(
                    event.getHospitalLatitude(), event.getHospitalLongitude(),
                    warehouse.getLatitude(), warehouse.getLongitude()
            );

            // Fetch available batches locked with pessimistic write lock (ordered by earliest expiry)
            List<InventoryBatch> batches = inventoryBatchRepository.findAvailableBatchesForAllocationLocked(
                    warehouse.getId(),
                    event.getItemType(),
                    LocalDate.now().plusDays(1) // must be valid at least until tomorrow
            );

            int totalAvailable = batches.stream().mapToInt(InventoryBatch::getAvailableQuantity).sum();
            if (totalAvailable <= 0) {
                continue; // No stock at this warehouse, check next closest
            }

            // Lock an available transport vehicle at this warehouse
            List<TransportVehicle> availableVehicles = transportVehicleRepository.findAvailableVehiclesLocked(
                    warehouse.getId(),
                    VehicleStatus.AVAILABLE,
                    100.0 // Minimum 100kg payload
            );

            if (availableVehicles.isEmpty()) {
                log.warn("Warehouse {} has stock but no AVAILABLE transport vehicles. Checking next warehouse.", warehouse.getName());
                continue;
            }

            TransportVehicle assignedVehicle = availableVehicles.get(0);

            // 3. Deduct stock using FEFO order
            int quantityToFulfill = Math.min(event.getRequestedQuantity(), totalAvailable);
            int remainingToDeduct = quantityToFulfill;

            for (InventoryBatch batch : batches) {
                if (remainingToDeduct <= 0) break;

                int batchAvailable = batch.getAvailableQuantity();
                int take = Math.min(remainingToDeduct, batchAvailable);

                batch.setReservedQuantity(batch.getReservedQuantity() + take);
                remainingToDeduct -= take;
                inventoryBatchRepository.save(batch);
                log.info("Deducted {} units from Batch {} (Expiry: {}) at Warehouse {}",
                        take, batch.getBatchNumber(), batch.getExpiryDate(), warehouse.getName());
            }

            // 4. Update vehicle status
            assignedVehicle.setStatus(VehicleStatus.EN_ROUTE);
            transportVehicleRepository.save(assignedVehicle);

            // 5. Calculate ETA based on distance
            long transitMinutes = (long) ((distanceKm / AVERAGE_TRANSIT_SPEED_KMH) * 60);
            Instant eta = Instant.now().plus(Duration.ofMinutes(Math.max(10, transitMinutes)));

            // 6. Create and persist DispatchOrder
            DispatchOrder order = DispatchOrder.builder()
                    .targetHospitalId(event.getHospitalId())
                    .sourceWarehouse(warehouse)
                    .itemType(event.getItemType())
                    .requestedQuantity(event.getRequestedQuantity())
                    .dispatchedQuantity(quantityToFulfill)
                    .assignedVehicle(assignedVehicle)
                    .status(DispatchStatus.DISPATCHED)
                    .estimatedArrivalTime(eta)
                    .build();

            DispatchOrder savedOrder = dispatchOrderRepository.save(order);
            log.info("Created DispatchOrder ID: {} for Hospital: {}, Dispatched: {} units",
                    savedOrder.getId(), event.getHospitalId(), quantityToFulfill);

            // 7. Publish Event to Kafka
            SupplyDispatchedEvent dispatchedEvent = SupplyDispatchedEvent.builder()
                    .dispatchOrderId(savedOrder.getId())
                    .targetHospitalId(savedOrder.getTargetHospitalId())
                    .sourceWarehouseId(warehouse.getId())
                    .warehouseName(warehouse.getName())
                    .itemType(savedOrder.getItemType())
                    .requestedQuantity(savedOrder.getRequestedQuantity())
                    .dispatchedQuantity(savedOrder.getDispatchedQuantity())
                    .vehicleId(assignedVehicle.getId())
                    .vehicleNumber(assignedVehicle.getVehicleNumber())
                    .status(savedOrder.getStatus())
                    .estimatedArrivalTime(savedOrder.getEstimatedArrivalTime())
                    .dispatchedAt(Instant.now())
                    .build();

            kafkaTemplate.send(SUPPLY_DISPATCHED_TOPIC, savedOrder.getId().toString(), dispatchedEvent);
            log.info("Published SupplyDispatchedEvent to topic [{}]", SUPPLY_DISPATCHED_TOPIC);

            return Optional.of(savedOrder);
        }

        log.error("CRITICAL: Failed to fulfill shortage alert for item {} across all warehouses!", event.getItemType());
        return Optional.empty();
    }
}