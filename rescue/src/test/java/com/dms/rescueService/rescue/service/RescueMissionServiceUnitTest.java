package com.dms.rescueService.rescue.service;

import com.dms.common.events.RescueMissionStatusUpdatedEvent;
import com.dms.rescueService.rescue.dto.request.MissionActionRequest;
import com.dms.rescueService.rescue.dto.response.RescueMissionResponse;
import com.dms.rescueService.rescue.entity.MissionStatus;
import com.dms.rescueService.rescue.entity.RescueDepartment;
import com.dms.rescueService.rescue.entity.RescueMission;
import com.dms.rescueService.rescue.entity.RescuePersonnel;
import com.dms.rescueService.rescue.repository.RescueDepartmentRepository;
import com.dms.rescueService.rescue.repository.RescueMissionRepository;
import com.dms.rescueService.rescue.repository.RescuePersonnelRepository;
import com.dms.rescueService.rescue.security.RescueAuthorizationService;
import com.dms.rescueService.rescue.service.impl.RescueMissionServiceImpl;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RescueMissionServiceUnitTest {

    @Mock
    private RescueMissionRepository missionRepository;

    @Mock
    private RescueDepartmentRepository departmentRepository;

    @Mock
    private RescuePersonnelRepository personnelRepository;

    @Mock
    private RedisGeoService redisGeoService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private RescueAuthorizationService authorization;

    private RescueMissionServiceImpl missionService;

    private final UUID missionId = UUID.randomUUID();
    private final UUID departmentId = UUID.randomUUID();
    private final UUID chiefId = UUID.randomUUID();
    private final UUID incidentId = UUID.randomUUID();

    private RescueMission mission;
    private RescueDepartment department;
    private RescuePersonnel chief;

    @BeforeEach
    void setUp() {
        missionService = new RescueMissionServiceImpl(
                missionRepository,
                departmentRepository,
                personnelRepository,
                redisGeoService,
                kafkaTemplate,
                authorization
        );
        ReflectionTestUtils.setField(missionService, "statusTopic", "rescue-mission-status-topic");

        department = new RescueDepartment();
        department.setId(departmentId);
        department.setName("Gorakhpur Fire Brigade");
        department.setActiveMissionsCount(1);
        department.setTotalCapacity(5);

        chief = new RescuePersonnel();
        chief.setId(chiefId);
        chief.setFullName("Captain Sharma");
        chief.setIsAvailable(false);

        mission = new RescueMission();
        mission.setId(missionId);
        mission.setDepartment(department);
        mission.setAssignedLeaderId(chiefId);
        mission.setIncidentId(incidentId);
        mission.setStatus(MissionStatus.ON_SCENE);

        when(missionRepository.findById(missionId)).thenReturn(Optional.of(mission));
        when(missionRepository.save(any(RescueMission.class))).thenAnswer(i -> i.getArgument(0));
        when(personnelRepository.findById(chiefId)).thenReturn(Optional.of(chief));
        when(departmentRepository.save(any(RescueDepartment.class))).thenAnswer(i -> i.getArgument(0));
        when(personnelRepository.save(any(RescuePersonnel.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Complete mission from ON_SCENE transitions to COMPLETED, releases chief and capacity, cleans spatial cache")
    void testCompleteMissionSuccess() {
        MissionActionRequest request = new MissionActionRequest();
        request.setNotes("All victims rescued and area secured.");

        RescueMissionResponse response = missionService.completeMission(missionId, request);

        assertNotNull(response);
        assertEquals(MissionStatus.COMPLETED, response.getStatus());

        ArgumentCaptor<RescueMission> captor = ArgumentCaptor.forClass(RescueMission.class);
        verify(missionRepository).save(captor.capture());
        RescueMission saved = captor.getValue();
        assertEquals(MissionStatus.COMPLETED, saved.getStatus());
        assertNotNull(saved.getCompletedAt());

        // Capacity released
        assertEquals(0, department.getActiveMissionsCount());
        verify(departmentRepository).save(department);

        // Chief released
        assertTrue(chief.getIsAvailable());
        verify(personnelRepository).save(chief);

        // Spatial RAM data cleared
        verify(redisGeoService).removeSpatialData(departmentId, incidentId);

        // Kafka event broadcast
        verify(kafkaTemplate).send(eq("rescue-mission-status-topic"), eq(incidentId.toString()), any(RescueMissionStatusUpdatedEvent.class));
    }

    @Test
    @DisplayName("Completing a mission directly from DISPATCHED state violates state machine and throws IllegalStateException")
    void testCompleteMissionInvalidStateThrows() {
        mission.setStatus(MissionStatus.DISPATCHED);

        MissionActionRequest request = new MissionActionRequest();
        request.setNotes("Premature completion");

        assertThrows(IllegalStateException.class, () ->
                missionService.completeMission(missionId, request));

        verify(missionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancel mission from DISPATCHED transitions to CANCELLED and releases station resources")
    void testCancelMissionFromDispatched() {
        mission.setStatus(MissionStatus.DISPATCHED);

        MissionActionRequest request = new MissionActionRequest();
        request.setReason("False alarm reported by district authority");

        RescueMissionResponse response = missionService.cancelMission(missionId, request);

        assertNotNull(response);
        assertEquals(MissionStatus.CANCELLED, response.getStatus());
        assertEquals(0, department.getActiveMissionsCount());
        assertTrue(chief.getIsAvailable());
        verify(redisGeoService).removeSpatialData(departmentId, incidentId);
    }

    @Test
    @DisplayName("Telemetry tick triggers state transition to ON_SCENE when unit arrives within 50m")
    void testTelemetryArrivalTransitionsToOnScene() {
        mission.setStatus(MissionStatus.EN_ROUTE);

        // Distance is 30 meters (<= 50 meters arrival threshold)
        when(redisGeoService.getDistanceToIncidentInMeters(departmentId, incidentId)).thenReturn(30.0);

        missionService.processLocationTelemetry(missionId, 26.7725, 83.1812);

        verify(redisGeoService).updateUnitLocation(departmentId, 26.7725, 83.1812);
        ArgumentCaptor<RescueMission> captor = ArgumentCaptor.forClass(RescueMission.class);
        verify(missionRepository).save(captor.capture());
        assertEquals(MissionStatus.ON_SCENE, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Attempting operation on non-existent mission ID throws RuntimeException")
    void testNonExistentMissionThrows() {
        UUID unknownId = UUID.randomUUID();
        when(missionRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                missionService.completeMission(unknownId, new MissionActionRequest()));
    }
}
