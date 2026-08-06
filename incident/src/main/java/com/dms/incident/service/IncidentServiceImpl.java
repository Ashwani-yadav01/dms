package com.dms.incident.service;

import com.dms.incident.dto.request.IncidentRequest;
import com.dms.incident.dto.response.IncidentResponse;
import com.dms.incident.entity.Incident;
import com.dms.incident.entity.IncidentStatus;
import com.dms.incident.entity.Severity;
import com.dms.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class IncidentServiceImpl implements IncidentService {
    private final ModelMapper mapper;
    private final IncidentRepository repository;
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculates distance between two lat/lng pairs in meters.
     */
    public static double calculateDistanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c * 1000.0; // Convert KM to Meters
    }

    /**
     * Helper check for 50 meters.
     */
    public static boolean isWithin50Meters(double lat1, double lon1, double lat2, double lon2) {
        return calculateDistanceInMeters(lat1, lon1, lat2, lon2) <= 50.0;
    }

    @Override
    public IncidentResponse createIncident(IncidentRequest request, UUID userId) {

        Incident newIncident = mapper.map(request, Incident.class);
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
