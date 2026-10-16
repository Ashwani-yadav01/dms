package com.dms.logisticsService.logistics.service;

import com.dms.logisticsService.logistics.dto.event.SupplyDeliveredEvent;
import com.dms.logisticsService.logistics.entity.DispatchOrder;
import com.dms.logisticsService.logistics.entity.TransportVehicle;
import com.dms.logisticsService.logistics.entity.Warehouse;
import com.dms.logisticsService.logistics.entity.enums.DispatchStatus;
import com.dms.logisticsService.logistics.entity.enums.ItemType;
import com.dms.logisticsService.logistics.entity.enums.VehicleStatus;
import com.dms.logisticsService.logistics.repository.DispatchOrderRepository;
import com.dms.logisticsService.logistics.repository.InventoryBatchRepository;
import com.dms.logisticsService.logistics.repository.TransportVehicleRepository;
import com.dms.logisticsService.logistics.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogisticsManagementServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private InventoryBatchRepository inventoryBatchRepository;

    @Mock
    private TransportVehicleRepository transportVehicleRepository;

    @Mock
    private DispatchOrderRepository dispatchOrderRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private LogisticsManagementService managementService;

    private UUID orderId;
    private UUID hospitalId;
    private DispatchOrder order;
    private TransportVehicle vehicle;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(managementService, "supplyDeliveredTopic", "supply-delivered-topic");

        orderId = UUID.randomUUID();
        hospitalId = UUID.randomUUID();

        warehouse = Warehouse.builder()
                .id(UUID.randomUUID())
                .name("Regional Logistics Depot")
                .latitude(28.6139)
                .longitude(77.2090)
                .isActive(true)
                .build();

        vehicle = TransportVehicle.builder()
                .id(UUID.randomUUID())
                .vehicleNumber("DL-01-LOG-1234")
                .status(VehicleStatus.EN_ROUTE)
                .baseWarehouse(warehouse)
                .build();

        order = DispatchOrder.builder()
                .id(orderId)
                .targetHospitalId(hospitalId)
                .sourceWarehouse(warehouse)
                .itemType(ItemType.OXYGEN_CYLINDER)
                .requestedQuantity(100)
                .dispatchedQuantity(80)
                .assignedVehicle(vehicle)
                .status(DispatchStatus.DISPATCHED)
                .build();
    }

    @Test
    void testMarkOrderAsDelivered_Success() {
        when(dispatchOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(dispatchOrderRepository.save(any(DispatchOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DispatchOrder delivered = managementService.markOrderAsDelivered(orderId);

        // Verify order status updated to DELIVERED
        assertEquals(DispatchStatus.DELIVERED, delivered.getStatus());

        // Verify vehicle freed to AVAILABLE
        assertEquals(VehicleStatus.AVAILABLE, vehicle.getStatus());
        verify(transportVehicleRepository, times(1)).save(vehicle);

        // Verify Kafka event published
        ArgumentCaptor<SupplyDeliveredEvent> eventCaptor = ArgumentCaptor.forClass(SupplyDeliveredEvent.class);
        verify(kafkaTemplate, times(1)).send(eq("supply-delivered-topic"), eq(orderId.toString()), eventCaptor.capture());

        SupplyDeliveredEvent publishedEvent = eventCaptor.getValue();
        assertEquals(orderId, publishedEvent.getDispatchOrderId());
        assertEquals(hospitalId, publishedEvent.getTargetHospitalId());
        assertEquals(ItemType.OXYGEN_CYLINDER, publishedEvent.getItemType());
        assertEquals(80, publishedEvent.getDeliveredQuantity());
        assertEquals(100, publishedEvent.getRequestedQuantity());
        assertEquals("DL-01-LOG-1234", publishedEvent.getVehicleNumber());
        assertEquals(DispatchStatus.DELIVERED, publishedEvent.getStatus());
    }

    @Test
    void testMarkOrderAsDelivered_AlreadyDelivered() {
        order.setStatus(DispatchStatus.DELIVERED);
        when(dispatchOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        DispatchOrder result = managementService.markOrderAsDelivered(orderId);

        assertEquals(DispatchStatus.DELIVERED, result.getStatus());
        verify(transportVehicleRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}
