package com.dms.hospitalService.hospital.service;

import com.dms.hospitalService.hospital.dto.request.HospitalBedUpdateRequest;
import com.dms.hospitalService.hospital.dto.response.HospitalResponse;
import com.dms.hospitalService.hospital.entity.FacilityType;
import com.dms.hospitalService.hospital.entity.Hospital;
import com.dms.hospitalService.hospital.entity.HospitalStatus;
import com.dms.hospitalService.hospital.repository.HospitalAuditRepository;
import com.dms.hospitalService.hospital.repository.HospitalRepository;
import com.dms.hospitalService.hospital.service.impl.HospitalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HospitalBedManagementUnitTest {

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private HospitalRedisGeoService geoService;

    @Mock
    private HospitalAuditRepository auditRepository;

    private HospitalServiceImpl hospitalService;

    private final UUID hospitalId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private Hospital hospital;

    @BeforeEach
    void setUp() {
        hospitalService = new HospitalServiceImpl(
                hospitalRepository,
                geoService,
                auditRepository
        );

        hospital = Hospital.builder()
                .id(hospitalId)
                .name("Community Health Centre Sahjanwa")
                .type(FacilityType.GENERAL_HOSPITAL)
                .latitude(26.7725)
                .longitude(83.1812)
                .totalGeneralBeds(40)
                .availableGeneralBeds(20)
                .totalIcuBeds(10)
                .availableIcuBeds(5)
                .isAcceptingPatients(true)
                .status(HospitalStatus.ACTIVE)
                .specialities(new HashSet<>())
                .bedLastUpdatedAt(LocalDateTime.now().minusHours(1))
                .build();

        when(hospitalRepository.findByIdForUpdate(hospitalId)).thenReturn(Optional.of(hospital));
        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
        when(hospitalRepository.save(any(Hospital.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Valid bed capacity update updates available beds and records audit")
    void testUpdateBedCapacitySuccess() {
        HospitalBedUpdateRequest request = new HospitalBedUpdateRequest();
        request.setAvailableGeneralBeds(25);
        request.setAvailableIcuBeds(8);

        HospitalResponse response = hospitalService.updateBedCapacity(hospitalId, request);

        assertNotNull(response);
        assertEquals(25, response.getAvailableGeneralBeds());
        assertEquals(8, response.getAvailableIcuBeds());

        ArgumentCaptor<Hospital> captor = ArgumentCaptor.forClass(Hospital.class);
        verify(hospitalRepository).save(captor.capture());
        Hospital saved = captor.getValue();

        assertEquals(25, saved.getAvailableGeneralBeds());
        assertEquals(8, saved.getAvailableIcuBeds());
        assertNotNull(saved.getBedLastUpdatedAt());
        verify(auditRepository).save(any());
    }

    @Test
    @DisplayName("Setting available general beds exceeding total capacity throws IllegalArgumentException")
    void testAvailableGeneralBedsExceedingTotalThrows() {
        HospitalBedUpdateRequest request = new HospitalBedUpdateRequest();
        request.setAvailableGeneralBeds(45); // Total is 40
        request.setAvailableIcuBeds(5);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                hospitalService.updateBedCapacity(hospitalId, request));

        assertTrue(ex.getMessage().contains("cannot exceed total general beds"));
        verify(hospitalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Setting available ICU beds exceeding total capacity throws IllegalArgumentException")
    void testAvailableIcuBedsExceedingTotalThrows() {
        HospitalBedUpdateRequest request = new HospitalBedUpdateRequest();
        request.setAvailableGeneralBeds(20);
        request.setAvailableIcuBeds(15); // Total is 10

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                hospitalService.updateBedCapacity(hospitalId, request));

        assertTrue(ex.getMessage().contains("cannot exceed total ICU beds"));
        verify(hospitalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Verify hospital transitions status to ACTIVE and registers GPS with Redis GEO")
    void testVerifyHospitalSuccess() {
        hospital.setStatus(HospitalStatus.PENDING_VERIFICATION);

        HospitalResponse response = hospitalService.verifyHospital(hospitalId, adminId, "DISTRICT_ADMIN");

        assertNotNull(response);
        assertEquals(HospitalStatus.ACTIVE, response.getStatus());
        verify(geoService).registerHospitalLocation(hospitalId, 26.7725, 83.1812);
        verify(hospitalRepository).save(hospital);
    }
}
