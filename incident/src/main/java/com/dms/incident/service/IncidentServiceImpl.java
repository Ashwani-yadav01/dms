package com.dms.incident.service;

import com.dms.incident.dto.request.IncidentRequest;
import com.dms.incident.dto.response.IncidentResponse;
import com.dms.incident.entity.Incident;
import com.dms.incident.entity.IncidentStatus;
import com.dms.incident.entity.Severity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
@RequiredArgsConstructor
@Service
public class IncidentServiceImpl implements IncidentService{
    private final ModelMapper mapper;


    @Override
    public IncidentResponse createIncident(IncidentRequest request, UUID userId) {
        return null;
    }

    @Override
    public IncidentResponse getIncidentById(UUID id) {
        return null;
    }

    @Override
    public Page<IncidentResponse> getAllIncidents(Pageable pageable) {
        return null;
    }

    @Override
    public Page<IncidentResponse> getIncidentsByUser(UUID userId, Pageable pageable) {
        return null;
    }

    @Override
    public Page<IncidentResponse> getIncidentsByStatus(IncidentStatus status, Pageable pageable) {
        return null;
    }

    @Override
    public Page<IncidentResponse> getIncidentsBySeverity(Severity severity, Pageable pageable) {
        return null;
    }

    @Override
    public Page<IncidentResponse> filterIncidents(IncidentStatus status, Severity severity, Pageable pageable) {
        return null;
    }

    @Override
    public List<IncidentResponse> getActiveIncidents() {
        return List.of();
    }

    @Override
    public List<IncidentResponse> getIncidentsInRadius(Double latitude, Double longitude, Double radiusInKm) {
        return List.of();
    }

    @Override
    public IncidentResponse updateIncidentStatus(UUID id, IncidentStatus status) {
        return null;
    }

    @Override
    public IncidentResponse updateIncident(UUID id, IncidentRequest request, UUID userId) {
        return null;
    }

    @Override
    public void deleteIncident(UUID id, UUID userId) {

    }
}
