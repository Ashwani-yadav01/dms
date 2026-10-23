package com.dms.logisticsService.logistics.service;

import com.dms.logisticsService.logistics.dto.event.InventoryShortageAlertEvent;
import com.dms.logisticsService.logistics.dto.event.SupplyDispatchedEvent;
import com.dms.logisticsService.logistics.entity.DispatchOrder;
import com.dms.logisticsService.logistics.entity.InventoryBatch;
import com.dms.logisticsService.logistics.entity.TransportVehicle;
import com.dms.logisticsService.logistics.entity.Warehouse;
import com.dms.logisticsService.logistics.entity.enums.DispatchStatus;
import com.dms.logisticsService.logistics.entity.enums.ItemType;
import com.dms.logisticsService.logistics.entity.enums.VehicleStatus;
import com.dms.logisticsService.logistics.entity.enums.VehicleType;
import com.dms.logisticsService.logistics.repository.DispatchOrderRepository;
import com.dms.logisticsService.logistics.repository.InventoryBatchRepository;
import com.dms.logisticsService.logistics.repository.TransportVehicleRepository;
import com.dms.logisticsService.logistics.repository.WarehouseRepository;
import com.dms.logisticsService.logistics.util.GeoDistanceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryAllocationServiceUnitTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private InventoryBatchRepository inventoryBatchRepository;

    @Mock
    private TransportVehicleRepository transportVehicleRepository;

    @Mock
    private DispatchOrderRepository dispatchOrderRepository;

    @Mock
    private GeoDistanceCalculator distanceCalculator;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private SupplyDispatchService dispatchService;

    private final UUID warehouseId = UUID.randomUUID();
    private final UUID hospitalId = UUID.randomUUID();
    private final UUID vehicleId = UUID.randomUUID();

    private Warehouse warehouse;
    private TransportVehicle vehicle;

    @BeforeEach
    void setUp() {
        dispatchService = new SupplyDispatchService(
                warehouseRepository,
                inventoryBatchRepository,
                transportVehicleRepository,
                dispatchOrderRepository,
                distanceCalculator,
                kafkaTemplate
        );

        warehouse = Warehouse.builder()
                .id(warehouseId)
                .name("Gorakhpur Regional Disaster Depot")
                .latitude(26.76)
                .longitude(83.37)
                .isActive(true)
                .build();

        vehicle = TransportVehicle.builder()
                .id(vehicleId)
                .vehicleNumber("UP-53-EM-9901")
                .type(VehicleType.TRUCK)
                .status(VehicleStatus.AVAILABLE)
                .maxPayloadKg(5000.0)
                .baseWarehouse(warehouse)
                .build();

        when(warehouseRepository.findByIsActiveTrue()).thenReturn(new ArrayList<>(List.of(warehouse)));
        when(distanceCalculator.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(15.0);
        when(transportVehicleRepository.findAvailableVehiclesLocked(eq(warehouseId), eq(VehicleStatus.AVAILABLE), anyDouble()))
                .thenReturn(new ArrayList<>(List.of(vehicle)));
        when(dispatchOrderRepository.save(any(DispatchOrder.class))).thenAnswer(i -> {
            DispatchOrder order = i.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });
    }

    @Test
    @DisplayName("FEFO inventory allocation prioritizes earlier expiry batch and reserves correct quantities")
    void testFefoBatchAllocation() {
        InventoryBatch batch1 = InventoryBatch.builder()
                .id(UUID.randomUUID())
                .warehouse(warehouse)
                .batchNumber("BATCH-EARLY")
                .itemType(ItemType.BLOOD_O_NEG)
                .quantity(30)
                .reservedQuantity(0)
                .expiryDate(LocalDate.now().plusDays(10))
                .build();

        InventoryBatch batch2 = InventoryBatch.builder()
                .id(UUID.randomUUID())
                .warehouse(warehouse)
                .batchNumber("BATCH-LATE")
                .itemType(ItemType.BLOOD_O_NEG)
                .quantity(50)
                .reservedQuantity(0)
                .expiryDate(LocalDate.now().plusDays(30))
                .build();

        // Locked repository query returns batches sorted by earliest expiry
        when(inventoryBatchRepository.findAvailableBatchesForAllocationLocked(eq(warehouseId), eq(ItemType.BLOOD_O_NEG), any(LocalDate.class)))
                .thenReturn(List.of(batch1, batch2));

        InventoryShortageAlertEvent event = InventoryShortageAlertEvent.builder()
                .hospitalId(hospitalId)
                .hospitalName("AIIMS Gorakhpur")
                .hospitalLatitude(26.75)
                .hospitalLongitude(83.40)
                .itemType(ItemType.BLOOD_O_NEG)
                .requestedQuantity(45)
                .build();

        Optional<DispatchOrder> result = dispatchService.processShortageAlert(event);

        assertTrue(result.isPresent());
        DispatchOrder order = result.get();
        assertEquals(DispatchStatus.DISPATCHED, order.getStatus());
        assertEquals(45, order.getDispatchedQuantity());

        // FEFO verification: Batch 1 is fully exhausted (30 reserved), Batch 2 supplies remaining 15 (15 reserved)
        assertEquals(30, batch1.getReservedQuantity());
        assertEquals(0, batch1.getAvailableQuantity());

        assertEquals(15, batch2.getReservedQuantity());
        assertEquals(35, batch2.getAvailableQuantity());

        // Vehicle dispatched
        assertEquals(VehicleStatus.EN_ROUTE, vehicle.getStatus());
        verify(transportVehicleRepository).save(vehicle);

        // Kafka dispatched event published
        verify(kafkaTemplate).send(eq("supply-dispatched-topic"), eq(order.getId().toString()), any(SupplyDispatchedEvent.class));
    }

    @Test
    @DisplayName("Shortage alert returns empty when no warehouse contains required item stock")
    void testShortageAlertNoStockReturnsEmpty() {
        when(inventoryBatchRepository.findAvailableBatchesForAllocationLocked(eq(warehouseId), any(ItemType.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        InventoryShortageAlertEvent event = InventoryShortageAlertEvent.builder()
                .hospitalId(hospitalId)
                .hospitalName("AIIMS Gorakhpur")
                .hospitalLatitude(26.75)
                .hospitalLongitude(83.40)
                .itemType(ItemType.VENTILATOR)
                .requestedQuantity(10)
                .build();

        Optional<DispatchOrder> result = dispatchService.processShortageAlert(event);

        assertTrue(result.isEmpty());
        verify(dispatchOrderRepository, never()).save(any());
        verify(transportVehicleRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Stock is not deducted if no transport vehicle is available at warehouse")
    void testNoTransportVehicleSkipsWarehouse() {
        InventoryBatch batch = InventoryBatch.builder()
                .id(UUID.randomUUID())
                .warehouse(warehouse)
                .batchNumber("BATCH-01")
                .itemType(ItemType.TRAUMA_SURGICAL_KIT)
                .quantity(100)
                .reservedQuantity(0)
                .expiryDate(LocalDate.now().plusDays(60))
                .build();

        when(inventoryBatchRepository.findAvailableBatchesForAllocationLocked(eq(warehouseId), eq(ItemType.TRAUMA_SURGICAL_KIT), any(LocalDate.class)))
                .thenReturn(List.of(batch));

        // No vehicles available
        when(transportVehicleRepository.findAvailableVehiclesLocked(eq(warehouseId), eq(VehicleStatus.AVAILABLE), anyDouble()))
                .thenReturn(Collections.emptyList());

        InventoryShortageAlertEvent event = InventoryShortageAlertEvent.builder()
                .hospitalId(hospitalId)
                .hospitalName("AIIMS Gorakhpur")
                .hospitalLatitude(26.75)
                .hospitalLongitude(83.40)
                .itemType(ItemType.TRAUMA_SURGICAL_KIT)
                .requestedQuantity(20)
                .build();

        Optional<DispatchOrder> result = dispatchService.processShortageAlert(event);

        assertTrue(result.isEmpty());
        assertEquals(0, batch.getReservedQuantity());
        assertEquals(100, batch.getAvailableQuantity());
        verify(dispatchOrderRepository, never()).save(any());
    }
}
