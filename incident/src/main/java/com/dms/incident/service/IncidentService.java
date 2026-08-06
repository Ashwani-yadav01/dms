package com.dms.incident.service;

import com.dms.incident.dto.request.IncidentRequest;
import com.dms.incident.dto.response.IncidentResponse;
import com.dms.incident.entity.IncidentStatus;
import com.dms.incident.entity.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface IncidentService {

    IncidentResponse createIncident(IncidentRequest request, UUID userId);

    IncidentResponse getIncidentById(UUID id);

    Page<IncidentResponse> getAllIncidents(Pageable pageable);

    Page<IncidentResponse> getIncidentsByUser(UUID userId, Pageable pageable);

    Page<IncidentResponse> getIncidentsByStatus(IncidentStatus status, Pageable pageable);

    Page<IncidentResponse> getIncidentsBySeverity(Severity severity, Pageable pageable);

    Page<IncidentResponse> filterIncidents(IncidentStatus status, Severity severity, Pageable pageable);

    List<IncidentResponse> getActiveIncidents();

    List<IncidentResponse> getIncidentsInRadius(Double latitude, Double longitude, Double radiusInKm);

    IncidentResponse updateIncidentStatus(UUID id, IncidentStatus status);

    IncidentResponse updateIncident(UUID id, IncidentRequest request, UUID userId);

    void deleteIncident(UUID id, UUID userId);
}