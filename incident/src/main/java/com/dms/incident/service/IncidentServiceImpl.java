package com.dms.incident.service;

import com.dms.incident.dto.request.IncidentRequest;
import com.dms.incident.dto.response.IncidentResponse;
import com.dms.incident.entity.Incident;
import com.dms.incident.entity.IncidentStatus;
import com.dms.incident.entity.Severity;
import com.dms.incident.exception.IncidentNotFoundException;
import com.dms.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentServiceImpl implements IncidentService {

    private final ModelMapper mapper;
    private final IncidentRepository repository;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DUPLICATE_RADIUS_METERS = 50.0;
    private static final List<IncidentStatus> ACTIVE_STATUSES = List.of(
            IncidentStatus.REPORTED,
            IncidentStatus.VERIFIED,
            IncidentStatus.DISPATCHED
    );

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
        return calculateDistanceInMeters(lat1, lon1, lat2, lon2) <= DUPLICATE_RADIUS_METERS;
    }

    @Override
    @Transactional
    public IncidentResponse createIncident(IncidentRequest request, UUID userId) {
        Incident newIncident = mapper.map(request, Incident.class);
        newIncident.setReportedBy(userId);
        newIncident.setStatus(IncidentStatus.REPORTED);

        // --- Deduplication Logic (50m Radius) ---
        // 1. Fetch nearby active incidents using a bounding box or Haversine query
        double radiusInKm = DUPLICATE_RADIUS_METERS / 1000.0;
        List<Incident> nearbyIncidents = repository.findNearbyActiveIncidents(
                request.getLatitude(),
                request.getLongitude(),
                radiusInKm,
                LocalDateTime.now().minusDays(1) // Window for recent active incidents
        );

        // 2. Find the closest active incident within 50 meters
        Optional<Incident> parentIncident = nearbyIncidents.stream()
                .filter(i -> isWithin50Meters(
                        request.getLatitude(), request.getLongitude(),
                        i.getLatitude(), i.getLongitude()
                ))
                .findFirst();

        // 3. If an existing active incident is found, mark this new incident as DUPLICATE and set parent ID
        if (parentIncident.isPresent()) {
            newIncident.setStatus(IncidentStatus.DUPLICATE);

            // If the matched incident is itself a duplicate, link to its parent root, otherwise link directly to it
            UUID rootParentId = parentIncident.get().getParentIncidentId() != null
                    ? parentIncident.get().getParentIncidentId()
                    : parentIncident.get().getId();

            newIncident.setParentIncidentId(rootParentId);
        }

        Incident savedIncident = repository.save(newIncident);
        return mapper.map(savedIncident, IncidentResponse.class);
    }

    @Override
    public IncidentResponse getIncidentById(UUID id) {
        Incident incident = findIncidentEntityById(id);
        return mapper.map(incident, IncidentResponse.class);
    }

    @Override
    public Page<IncidentResponse> getAllIncidents(Pageable pageable) {
        return repository.findAll(pageable)
                .map(entity -> mapper.map(entity, IncidentResponse.class));
    }

    @Override
    public Page<IncidentResponse> getIncidentsByUser(UUID userId, Pageable pageable) {
        return repository.findByReportedBy(userId, pageable)
                .map(entity -> mapper.map(entity, IncidentResponse.class));
    }

    @Override
    public Page<IncidentResponse> getIncidentsByStatus(IncidentStatus status, Pageable pageable) {
        return repository.findByStatus(status, pageable)
                .map(entity -> mapper.map(entity, IncidentResponse.class));
    }

    @Override
    public Page<IncidentResponse> getIncidentsBySeverity(Severity severity, Pageable pageable) {
        return repository.findBySeverity(severity, pageable)
                .map(entity -> mapper.map(entity, IncidentResponse.class));
    }

    @Override
    public Page<IncidentResponse> filterIncidents(IncidentStatus status, Severity severity, Pageable pageable) {
        if (status != null && severity != null) {
            return repository.findByStatusAndSeverity(status, severity, pageable)
                    .map(entity -> mapper.map(entity, IncidentResponse.class));
        } else if (status != null) {
            return getIncidentsByStatus(status, pageable);
        } else if (severity != null) {
            return getIncidentsBySeverity(severity, pageable);
        }
        return getAllIncidents(pageable);
    }

    @Override
    public List<IncidentResponse> getActiveIncidents() {
        return repository.findByStatusIn(ACTIVE_STATUSES)
                .stream()
                .map(entity -> mapper.map(entity, IncidentResponse.class))
                .toList();
    }

    @Override
    public List<IncidentResponse> getIncidentsInRadius(Double latitude, Double longitude, Double radiusInKm) {
        LocalDateTime timeThreshold = LocalDateTime.now().minusHours(24);
        return repository.findNearbyActiveIncidents(latitude, longitude, radiusInKm, timeThreshold)
                .stream()
                .map(entity -> mapper.map(entity, IncidentResponse.class))
                .toList();
    }

    @Override
    @Transactional
    public IncidentResponse updateIncidentStatus(UUID id, IncidentStatus status) {
        Incident incident = findIncidentEntityById(id);
        incident.setStatus(status);
        Incident updatedIncident = repository.save(incident);
        return mapper.map(updatedIncident, IncidentResponse.class);
    }

    @Override
    @Transactional
    public IncidentResponse updateIncident(UUID id, IncidentRequest request, UUID userId) {
        Incident incident = findIncidentEntityById(id);

        mapper.map(request, incident);
        incident.setReportedBy(userId); // Maintain reportedBy integrity

        Incident updatedIncident = repository.save(incident);
        return mapper.map(updatedIncident, IncidentResponse.class);
    }

    @Override
    @Transactional
    public void deleteIncident(UUID id, UUID userId) {
        Incident incident = findIncidentEntityById(id);
        repository.delete(incident);
    }

    // --- Private Helpers ---

    private Incident findIncidentEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident is not found with id " + id));
    }
}