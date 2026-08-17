package com.dms.incident.service;

import com.dms.common.events.IncidentCreatedEvent;
import com.dms.incident.dto.request.IncidentRequest;
import com.dms.incident.dto.response.IncidentResponse;
import com.dms.incident.entity.Incident;
import com.dms.incident.entity.IncidentStatus;
import com.dms.incident.entity.Severity;
import com.dms.incident.exception.IncidentNotFoundException;
import com.dms.incident.messaging.IncidentEventPublisher;
import com.dms.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentServiceImpl implements IncidentService {

    private final ModelMapper mapper;
    private final IncidentRepository repository;
    private final IncidentEventPublisher incidentEventPublisher;
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

        // --- Deduplication Logic (50m Radius + Same Incident Type) ---
        double radiusInKm = DUPLICATE_RADIUS_METERS / 1000.0;
        List<Incident> nearbyIncidents = repository.findNearbyActiveIncidents(
                request.getLatitude(),
                request.getLongitude(),
                radiusInKm,
                LocalDateTime.now().minusDays(1)
        );

        Optional<Incident> parentIncident = nearbyIncidents.stream()
                .filter(i -> i.getIncidentType() == request.getIncidentType())
                .filter(i -> isWithin50Meters(
                        request.getLatitude(), request.getLongitude(),
                        i.getLatitude(), i.getLongitude()
                ))
                .findFirst();

        if (parentIncident.isPresent()) {
            newIncident.setStatus(IncidentStatus.DUPLICATE);

            UUID rootParentId = parentIncident.get().getParentIncidentId() != null
                    ? parentIncident.get().getParentIncidentId()
                    : parentIncident.get().getId();

            newIncident.setParentIncidentId(rootParentId);
        }

        Incident savedIncident = repository.save(newIncident);

        // --- PUBLISH KAFKA EVENT IF NOT A DUPLICATE ---
        if (savedIncident.getStatus() != IncidentStatus.DUPLICATE) {
            IncidentCreatedEvent event = IncidentCreatedEvent.builder()
                    .incidentId(savedIncident.getId())
                    .title(savedIncident.getTitle())
                    .description(savedIncident.getDescription())
                    .incidentType(savedIncident.getIncidentType().name())
                    .severity(savedIncident.getSeverity().name())
                    .latitude(savedIncident.getLatitude())
                    .longitude(savedIncident.getLongitude())
                    .reportedBy(savedIncident.getReportedBy())
                    .createdAt(savedIncident.getCreatedAt())
                    .build();

            incidentEventPublisher.publishIncidentCreated(event);
        }

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
        incident.setReportedBy(userId);

        Incident updatedIncident = repository.save(incident);
        return mapper.map(updatedIncident, IncidentResponse.class);
    }

    @Override
    @Transactional
    public void deleteIncident(UUID id, UUID userId) {
        Incident incident = findIncidentEntityById(id);
        repository.delete(incident);
    }

    @Override
    @Transactional
    public IncidentResponse updateStatusFromRescueEvent(UUID incidentId, String status, String notes) {
        Incident incident = findIncidentEntityById(incidentId);

        if (incident.getStatus().isTerminal()) {
            log.info("Incident ID: {} is already in terminal state ({}). Skipping status transition from rescue event.",
                    incidentId, incident.getStatus());
            return mapper.map(incident, IncidentResponse.class);
        }

        try {
            IncidentStatus newStatus = IncidentStatus.valueOf(status);
            incident.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            log.warn("Received status string [{}] does not directly map to IncidentStatus enum. Retaining current status.", status);
        }

        if (notes != null && !notes.isBlank()) {
            String existingDescription = incident.getDescription() != null ? incident.getDescription() : "";
            incident.setDescription(existingDescription + "\n[Rescue Update]: " + notes.trim());
        }

        Incident savedIncident = repository.save(incident);
        log.info("Updated Incident ID: {} status to {} based on Rescue Event.", incidentId, savedIncident.getStatus());

        return mapper.map(savedIncident, IncidentResponse.class);
    }

    @Override
    @Transactional
    public void processRescueMissionCompletion(UUID incidentId, UUID missionId, String resolutionNotes) {
        Incident incident = findIncidentEntityById(incidentId);

        if (incident.getStatus().isTerminal()) {
            log.info("Incident ID: {} is already in terminal state ({}). Skipping state transition.",
                    incidentId, incident.getStatus());
            return;
        }

        incident.setStatus(IncidentStatus.RESOLVED);

        String existingDescription = incident.getDescription() != null ? incident.getDescription() : "";
        String noteSummary = String.format("\n[RESOLVED via Rescue Mission %s]: %s",
                missionId, resolutionNotes != null ? resolutionNotes : "Completed successfully.");
        incident.setDescription(existingDescription + noteSummary);

        repository.save(incident);
        log.info("Incident ID: {} status updated to RESOLVED via Kafka event.", incidentId);
    }

    private Incident findIncidentEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident is not found with id " + id));
    }
    @Override
    public void notifyEndUser(UUID incidentId, String status, String notes) {
        log.info("Notifying end-user for Incident ID: {} with new Status: {} and Notes: {}",
                incidentId, status, notes);
    }
}