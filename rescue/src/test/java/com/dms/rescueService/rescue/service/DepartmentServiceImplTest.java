package com.dms.rescueService.rescue.service;

import com.dms.rescueService.rescue.dto.request.DepartmentCreateRequest;
import com.dms.rescueService.rescue.dto.request.DepartmentUpdateRequest;
import com.dms.rescueService.rescue.dto.response.DepartmentResponse;
import com.dms.rescueService.rescue.entity.DepartmentType;
import com.dms.rescueService.rescue.entity.RescueDepartment;
import com.dms.rescueService.rescue.repository.RescueDepartmentRepository;
import com.dms.rescueService.rescue.repository.RescueMissionRepository;
import com.dms.rescueService.rescue.repository.RescuePersonnelRepository;
import com.dms.rescueService.rescue.security.RescueAuthorizationService;
import com.dms.rescueService.rescue.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private RescueDepartmentRepository departmentRepository;

    @Mock
    private RescueMissionRepository missionRepository;

    @Mock
    private RescuePersonnelRepository personnelRepository;

    @Mock
    private RescueAuthorizationService authorization;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private UUID adminId;
    private UUID chiefId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        chiefId = UUID.randomUUID();
    }

    @Test
    void createDepartment_failsWhenStationChiefAlreadyAssignedToAnotherStation() {
        DepartmentCreateRequest request = new DepartmentCreateRequest();
        request.setName("New Fire Station");
        request.setType(DepartmentType.FIRE_STATION);
        request.setJurisdictionCode("DIST-GKP-01");
        request.setLatitude(26.76);
        request.setLongitude(83.37);
        request.setContactPhone("+919876543210");
        request.setTotalCapacity(15);
        request.setStationChiefId(chiefId);

        RescueDepartment existingStation = RescueDepartment.builder()
                .id(UUID.randomUUID())
                .name("Existing Central Fire Station")
                .stationChiefId(chiefId)
                .build();

        when(departmentRepository.findByStationChiefId(chiefId)).thenReturn(List.of(existingStation));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> departmentService.createDepartment(request));

        assertTrue(ex.getMessage().contains("already assigned as Station Chief"));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createDepartment_autoAssignsGoverningOfficialIdToDistrictAdmin() {
        DepartmentCreateRequest request = new DepartmentCreateRequest();
        request.setName("New NDRF Station");
        request.setType(DepartmentType.NDRF_BASE);
        request.setJurisdictionCode("DIST-GKP-02");
        request.setLatitude(26.78);
        request.setLongitude(83.39);
        request.setContactPhone("+919876543211");
        request.setTotalCapacity(20);
        request.setGoverningOfficialId(null); // Unset

        when(authorization.currentUserIdOrNull()).thenReturn(adminId);
        when(departmentRepository.save(any(RescueDepartment.class))).thenAnswer(invocation -> {
            RescueDepartment d = invocation.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        DepartmentResponse response = departmentService.createDepartment(request);

        assertNotNull(response);
        assertEquals(adminId, response.getGoverningOfficialId());
        assertEquals(adminId, response.getDistrictAdminId());
        verify(departmentRepository).save(any(RescueDepartment.class));
    }

    @Test
    void updateDepartment_failsWhenStationChiefAssignedToDifferentStation() {
        UUID stationAId = UUID.randomUUID();
        UUID stationBId = UUID.randomUUID();

        RescueDepartment stationA = RescueDepartment.builder()
                .id(stationAId)
                .name("Station A")
                .build();

        RescueDepartment stationB = RescueDepartment.builder()
                .id(stationBId)
                .name("Station B")
                .stationChiefId(chiefId)
                .build();

        when(departmentRepository.findById(stationAId)).thenReturn(Optional.of(stationA));
        when(departmentRepository.findByStationChiefId(chiefId)).thenReturn(List.of(stationB));

        DepartmentUpdateRequest updateRequest = new DepartmentUpdateRequest();
        updateRequest.setStationChiefId(chiefId);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> departmentService.updateDepartment(stationAId, updateRequest));

        assertTrue(ex.getMessage().contains("already assigned as Station Chief"));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void updateDepartment_succeedsWhenKeepingSameChief() {
        UUID stationAId = UUID.randomUUID();

        RescueDepartment stationA = RescueDepartment.builder()
                .id(stationAId)
                .name("Station A")
                .stationChiefId(chiefId)
                .build();

        when(departmentRepository.findById(stationAId)).thenReturn(Optional.of(stationA));
        when(departmentRepository.findByStationChiefId(chiefId)).thenReturn(List.of(stationA));
        when(departmentRepository.save(any(RescueDepartment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DepartmentUpdateRequest updateRequest = new DepartmentUpdateRequest();
        updateRequest.setName("Station A Renamed");
        updateRequest.setStationChiefId(chiefId);

        DepartmentResponse response = departmentService.updateDepartment(stationAId, updateRequest);

        assertNotNull(response);
        assertEquals("Station A Renamed", response.getName());
        assertEquals(chiefId, response.getStationChiefId());
        verify(departmentRepository).save(stationA);
    }
}
