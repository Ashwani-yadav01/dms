package com.dms.hospitalService.hospital.controller;

import com.dms.hospitalService.hospital.dto.response.HospitalResponse;
import com.dms.hospitalService.hospital.entity.DistrictMedicalReserve;
import com.dms.hospitalService.hospital.entity.HospitalSupplyRequest;
import com.dms.hospitalService.hospital.entity.InventoryItemType;
import com.dms.hospitalService.hospital.entity.SupplyRequestStatus;
import com.dms.hospitalService.hospital.repository.DistrictMedicalReserveRepository;
import com.dms.hospitalService.hospital.repository.HospitalDeliveryReceiptRepository;
import com.dms.hospitalService.hospital.repository.HospitalSupplyRequestRepository;
import com.dms.hospitalService.hospital.repository.MedicalInventoryRepository;
import com.dms.hospitalService.hospital.service.HospitalInventoryAlertService;
import com.dms.hospitalService.hospital.service.HospitalService;
import com.dms.hospitalService.hospital.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HospitalSupplyRequestTest {

    @Mock
    private HospitalInventoryAlertService alertService;
    @Mock
    private HospitalService hospitalService;
    @Mock
    private HospitalDeliveryReceiptRepository deliveryReceiptRepository;
    @Mock
    private HospitalSupplyRequestRepository supplyRequestRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private MedicalInventoryRepository medicalInventoryRepository;
    @Mock
    private DistrictMedicalReserveRepository districtReserveRepository;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private HospitalInventoryController controller;

    private UUID hospitalId;
    private UUID requestId;

    @BeforeEach
    void setup() {
        hospitalId = UUID.randomUUID();
        requestId = UUID.randomUUID();
    }

    @Test
    void testApproveSupplyRequest_Success() {
        when(request.getHeader("X-User-Role")).thenReturn("DISTRICT_ADMIN");
        when(request.getHeader("X-User-Department-Category")).thenReturn("MEDICAL_MANAGEMENT");
        when(request.getHeader("X-User-Id")).thenReturn(UUID.randomUUID().toString());

        HospitalSupplyRequest existing = HospitalSupplyRequest.builder()
                .id(requestId)
                .hospitalId(hospitalId)
                .hospitalName("District General Hospital")
                .itemType("O_NEGATIVE_BLOOD")
                .requestedQuantity(40)
                .urgencyLevel("CRITICAL")
                .status(SupplyRequestStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        DistrictMedicalReserve initialReserve = DistrictMedicalReserve.builder()
                .itemType(InventoryItemType.O_NEGATIVE_BLOOD)
                .currentQuantity(500)
                .build();

        when(supplyRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        when(supplyRequestRepository.save(any(HospitalSupplyRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(districtReserveRepository.findByItemType(InventoryItemType.O_NEGATIVE_BLOOD)).thenReturn(Optional.of(initialReserve));

        ResponseEntity<?> response = controller.approveSupplyRequest(request, requestId);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof HospitalSupplyRequest);
        HospitalSupplyRequest result = (HospitalSupplyRequest) response.getBody();

        assertEquals(SupplyRequestStatus.APPROVED, result.getStatus());
        assertNotNull(result.getActionedAt());
        assertNotNull(result.getActionedByAdminId());

        // Verify district reserves stock was DEDUCTED (500 - 40 = 460)
        assertEquals(460, initialReserve.getCurrentQuantity());
        verify(districtReserveRepository).save(initialReserve);

        // Verify hospital local inventory was restocked
        verify(inventoryService).restockItemFromDispatch(hospitalId, "O_NEGATIVE_BLOOD", 40);
        verify(deliveryReceiptRepository).save(any());
    }

    @Test
    void testRejectSupplyRequest_Success() {
        when(request.getHeader("X-User-Role")).thenReturn("DISTRICT_ADMIN");
        when(request.getHeader("X-User-Department-Category")).thenReturn("MEDICAL_MANAGEMENT");
        when(request.getHeader("X-User-Id")).thenReturn(UUID.randomUUID().toString());

        HospitalSupplyRequest existing = HospitalSupplyRequest.builder()
                .id(requestId)
                .hospitalId(hospitalId)
                .hospitalName("District General Hospital")
                .itemType("OXYGEN_CYLINDER")
                .requestedQuantity(20)
                .urgencyLevel("HIGH")
                .status(SupplyRequestStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(supplyRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        when(supplyRequestRepository.save(any(HospitalSupplyRequest.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<?> response = controller.rejectSupplyRequest(request, requestId, "Depleted district reserves");

        assertEquals(200, response.getStatusCode().value());
        HospitalSupplyRequest result = (HospitalSupplyRequest) response.getBody();

        assertEquals(SupplyRequestStatus.REJECTED, result.getStatus());
        assertEquals("Depleted district reserves", result.getAdminNotes());
        verify(inventoryService, never()).restockItemFromDispatch(any(), any(), anyInt());
    }

    @Test
    void testDeclareShortage_CreatesPendingRequest() {
        when(request.getHeader("X-User-Role")).thenReturn("HOSPITAL");
        when(request.getHeader("X-User-Id")).thenReturn(UUID.randomUUID().toString());

        HospitalResponse mockHospital = HospitalResponse.builder()
                .id(hospitalId)
                .name("City Care Hospital")
                .adminUserId(UUID.fromString(request.getHeader("X-User-Id")))
                .build();

        when(hospitalService.getHospitalById(hospitalId)).thenReturn(mockHospital);
        when(supplyRequestRepository.save(any(HospitalSupplyRequest.class))).thenAnswer(i -> i.getArgument(0));

        HospitalInventoryController.ShortageRequest body = new HospitalInventoryController.ShortageRequest(
                "O_NEGATIVE_BLOOD", 30, "CRITICAL"
        );

        ResponseEntity<?> response = controller.declareShortage(request, hospitalId, body);

        assertEquals(202, response.getStatusCode().value());
        HospitalSupplyRequest saved = (HospitalSupplyRequest) response.getBody();
        assertEquals(SupplyRequestStatus.PENDING, saved.getStatus());
        assertEquals("CRITICAL", saved.getUrgencyLevel());
        assertEquals(30, saved.getRequestedQuantity());
        verify(supplyRequestRepository).save(any(HospitalSupplyRequest.class));
    }
}
