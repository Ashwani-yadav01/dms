package com.dms.incident.service;

import com.dms.common.events.IncidentCreatedEvent;
import com.dms.incident.dto.request.IncidentRequest;
import com.dms.incident.dto.response.IncidentResponse;
import com.dms.incident.entity.Incident;
import com.dms.incident.entity.IncidentAction;
import com.dms.incident.entity.IncidentStatus;
import com.dms.incident.entity.IncidentType;
import com.dms.incident.entity.LocationSource;
import com.dms.incident.entity.LocationTrustLevel;
import com.dms.incident.entity.Severity;
import com.dms.incident.exception.IncidentNotFoundException;
import com.dms.incident.messaging.IncidentEventPublisher;
import com.dms.incident.repository.IncidentAuditRepository;
import com.dms.incident.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IncidentServiceUnitTest {

    @Mock
    private IncidentRepository repository;

    @Mock
    private IncidentEventPublisher incidentEventPublisher;

    @Mock
    private CloudinaryImageService cloudinaryImageService;

    @Mock
    private IncidentAuthorizationService authorization;

    @Mock
    private IncidentAuditRepository auditRepository;

    private ModelMapper mapper;
    private IncidentServiceImpl incidentService;

    private final UUID citizenId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mapper = new ModelMapper();
        incidentService = new IncidentServiceImpl(
                mapper,
                repository,
                incidentEventPublisher,
                cloudinaryImageService,
                authorization,
                auditRepository
        );

        when(authorization.userId()).thenReturn(citizenId);
    }

    @Test
    @DisplayName("Create incident with matching GPS coordinates receives MATCH trust level")
    void testCreateUniqueIncident() {
        IncidentRequest request = new IncidentRequest();
        request.setTitle("Kitchen fire in Sahjanwa");
        request.setDescription("Flames visible on first floor");
        request.setIncidentType(IncidentType.FIRE);
        request.setSeverity(Severity.MEDIUM);
        request.setLatitude(26.7725);
        request.setLongitude(83.1812);
        request.setDeviceLatitude(26.7726); // ~11 meters away
        request.setDeviceLongitude(83.1812);
        request.setLocationSource(LocationSource.BROWSER_GPS);

        when(repository.findNearbyActiveIncidents(anyDouble(), anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        when(repository.save(any(Incident.class))).thenAnswer(invocation -> {
            Incident saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        IncidentResponse response = incidentService.createIncident(request, citizenId);

        assertNotNull(response);
        assertEquals(IncidentStatus.REPORTED, response.getStatus());

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(repository).save(captor.capture());
        Incident captured = captor.getValue();

        assertEquals(citizenId, captured.getReportedBy());
        assertEquals(LocationTrustLevel.MATCH, captured.getLocationTrustLevel());
        assertNull(captured.getParentIncidentId());
    }

    @Test
    @DisplayName("Duplicate report within 50m of active incident is flagged as DUPLICATE and linked")
    void testDuplicateIncidentWithin50Meters() {
        UUID parentIncidentId = UUID.randomUUID();
        Incident existingIncident = new Incident();
        existingIncident.setId(parentIncidentId);
        existingIncident.setIncidentType(IncidentType.FIRE);
        existingIncident.setStatus(IncidentStatus.REPORTED);
        existingIncident.setLatitude(26.772500);
        existingIncident.setLongitude(83.181200);

        IncidentRequest duplicateRequest = new IncidentRequest();
        duplicateRequest.setTitle("Another report of the same fire");
        duplicateRequest.setIncidentType(IncidentType.FIRE);
        duplicateRequest.setLatitude(26.772600); // ~11 meters away
        duplicateRequest.setLongitude(83.181200);

        when(repository.findNearbyActiveIncidents(anyDouble(), anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(List.of(existingIncident));

        when(repository.save(any(Incident.class))).thenAnswer(invocation -> {
            Incident saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        incidentService.createIncident(duplicateRequest, citizenId);

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(repository).save(captor.capture());
        Incident captured = captor.getValue();

        assertEquals(IncidentStatus.DUPLICATE, captured.getStatus());
        assertEquals(parentIncidentId, captured.getParentIncidentId());
    }

    @Test
    @DisplayName("Reporting on behalf flags trust level as REMOTE_REPORT")
    void testReportingOnBehalfTrustLevel() {
        IncidentRequest request = new IncidentRequest();
        request.setTitle("Accident on bypass");
        request.setIncidentType(IncidentType.ACCIDENT);
        request.setLatitude(26.7725);
        request.setLongitude(83.1812);
        request.setIsReportingOnBehalf(true);
        request.setDeviceLatitude(26.8500); // distant location
        request.setDeviceLongitude(83.3500);

        when(repository.findNearbyActiveIncidents(anyDouble(), anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        when(repository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        incidentService.createIncident(request, citizenId);

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(repository).save(captor.capture());
        assertEquals(LocationTrustLevel.REMOTE_REPORT, captor.getValue().getLocationTrustLevel());
    }

    @Test
    @DisplayName("Verifying incident transitions status from REPORTED to VERIFIED and publishes Kafka event")
    void testVerifyIncidentSuccess() {
        UUID incidentId = UUID.randomUUID();
        Incident incident = new Incident();
        incident.setId(incidentId);
        incident.setTitle("Confirmed flood surge");
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setIncidentType(IncidentType.FLOOD);
        incident.setSeverity(Severity.HIGH);
        incident.setLatitude(26.75);
        incident.setLongitude(83.25);
        incident.setCreatedAt(LocalDateTime.now());

        when(repository.findByIdForUpdate(incidentId)).thenReturn(Optional.of(incident));
        when(repository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(authorization.userId()).thenReturn(adminId);
        when(repository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IncidentResponse response = incidentService.updateIncidentStatus(incidentId, IncidentStatus.VERIFIED, null);

        assertEquals(IncidentStatus.VERIFIED, response.getStatus());
        verify(incidentEventPublisher).publishIncidentCreated(any(IncidentCreatedEvent.class));
        verify(auditRepository).save(any());
    }

    @Test
    @DisplayName("Non-existent incident throws IncidentNotFoundException")
    void testGetNonExistentIncidentThrows() {
        UUID randomId = UUID.randomUUID();
        when(repository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(IncidentNotFoundException.class, () ->
                incidentService.getIncidentById(randomId));
    }
}
