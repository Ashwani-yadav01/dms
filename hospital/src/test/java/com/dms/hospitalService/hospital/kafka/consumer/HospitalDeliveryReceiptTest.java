package com.dms.hospitalService.hospital.kafka.consumer;

import com.dms.hospitalService.hospital.entity.HospitalDeliveryReceipt;
import com.dms.hospitalService.hospital.kafka.event.SupplyDeliveredEvent;
import com.dms.hospitalService.hospital.kafka.event.SupplyDispatchedEvent;
import com.dms.hospitalService.hospital.repository.HospitalDeliveryReceiptRepository;
import com.dms.hospitalService.hospital.service.InventoryService;
import com.dms.hospitalService.hospital.service.impl.HospitalSurgeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HospitalDeliveryReceiptTest {

    @Mock
    private HospitalSurgeServiceImpl hospitalSurgeService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private HospitalDeliveryReceiptRepository deliveryReceiptRepository;

    @InjectMocks
    private HospitalKafkaConsumer kafkaConsumer;

    private UUID hospitalId;
    private UUID dispatchOrderId;
    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        hospitalId = UUID.randomUUID();
        dispatchOrderId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
    }

    @Test
    void testConsumeSupplyDelivered_Success() {
        SupplyDeliveredEvent event = SupplyDeliveredEvent.builder()
                .dispatchOrderId(dispatchOrderId)
                .targetHospitalId(hospitalId)
                .sourceWarehouseId(warehouseId)
                .warehouseName("North Regional Depot")
                .itemType("O_NEGATIVE_BLOOD")
                .requestedQuantity(50)
                .deliveredQuantity(40)
                .vehicleNumber("DL-01-EM-9999")
                .status("DELIVERED")
                .deliveredAt(Instant.now())
                .build();

        when(deliveryReceiptRepository.existsByDispatchOrderId(dispatchOrderId)).thenReturn(false);

        kafkaConsumer.consumeSupplyDelivered(event);

        // Verify restocking was triggered
        verify(inventoryService, times(1)).restockItemFromDispatch(hospitalId, "O_NEGATIVE_BLOOD", 40);

        // Verify delivery receipt was persisted
        ArgumentCaptor<HospitalDeliveryReceipt> receiptCaptor = ArgumentCaptor.forClass(HospitalDeliveryReceipt.class);
        verify(deliveryReceiptRepository, times(1)).save(receiptCaptor.capture());

        HospitalDeliveryReceipt savedReceipt = receiptCaptor.getValue();
        assertEquals(dispatchOrderId, savedReceipt.getDispatchOrderId());
        assertEquals(hospitalId, savedReceipt.getHospitalId());
        assertEquals("North Regional Depot", savedReceipt.getWarehouseName());
        assertEquals(40, savedReceipt.getDeliveredQuantity());
        assertEquals("O_NEGATIVE_BLOOD", savedReceipt.getItemType());
    }

    @Test
    void testConsumeSupplyDelivered_DuplicateIgnored() {
        SupplyDeliveredEvent event = SupplyDeliveredEvent.builder()
                .dispatchOrderId(dispatchOrderId)
                .targetHospitalId(hospitalId)
                .deliveredQuantity(30)
                .itemType("VENTILATOR")
                .build();

        when(deliveryReceiptRepository.existsByDispatchOrderId(dispatchOrderId)).thenReturn(true);

        kafkaConsumer.consumeSupplyDelivered(event);

        // Verify restocking was NOT invoked due to idempotency check
        verify(inventoryService, never()).restockItemFromDispatch(any(), any(), anyInt());
        verify(deliveryReceiptRepository, never()).save(any());
    }

    @Test
    void testConsumeSupplyDispatched_DoesNotRestockEagerly() {
        SupplyDispatchedEvent event = SupplyDispatchedEvent.builder()
                .dispatchOrderId(dispatchOrderId)
                .targetHospitalId(hospitalId)
                .warehouseName("Central Depot")
                .itemType("OXYGEN_CYLINDER")
                .dispatchedQuantity(25)
                .vehicleNumber("UP-16-TR-4567")
                .status("DISPATCHED")
                .estimatedArrivalTime(Instant.now().plusSeconds(3600))
                .dispatchedAt(Instant.now())
                .build();

        kafkaConsumer.consumeSupplyDispatched(event);

        // Verify inventory is NOT restocked on dispatch notice (must wait for actual delivery)
        verify(inventoryService, never()).restockItemFromDispatch(any(), any(), anyInt());
        verify(deliveryReceiptRepository, never()).save(any());
    }
}
