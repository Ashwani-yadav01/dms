package com.dms.incident.service;

import com.dms.common.events.IncidentCreatedEvent;
import com.dms.incident.dto.request.IncidentRequest;
import com.dms.incident.dto.request.AssignOfficialRequest;
import com.dms.incident.dto.response.IncidentResponse;
import com.dms.incident.entity.Incident;
import com.dms.incident.entity.IncidentStatus;
import com.dms.incident.entity.IncidentAction;
import com.dms.incident.entity.Severity;
import com.dms.incident.exception.IncidentNotFoundException;
import com.dms.incident.exception.BadRequestException;
import com.dms.incident.exception.IncidentAuthorizationException;
import com.dms.incident.messaging.IncidentEventPublisher;
import com.dms.incident.repository.IncidentRepository;
import com.dms.incident.repository.IncidentAuditRepository;
import com.dms.incident.entity.IncidentAudit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final CloudinaryImageService cloudinaryImageService;
    private final IncidentAuthorizationService authorization;
    private final IncidentAuditRepository auditRepository;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DUPLICATE_RADIUS_METERS = 50.0;
    private static final List<IncidentStatus> ACTIVE_STATUSES = List.of(
            IncidentStatus.REPORTED,
            IncidentStatus.VERIFIED,
            IncidentStatus.DISPATCHED
    );

    public static double calculateDistanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c * 1000.0;
    }

    public static boolean isWithin50Meters(double lat1, double lon1, double lat2, double lon2) {
        return calculateDistanceInMeters(lat1, lon1, lat2, lon2) <= DUPLICATE_RADIUS_METERS;
    }

    @Override
    @Transactional
    public IncidentResponse createIncident(IncidentRequest request, UUID userId) {
        authorization.authorize(null, IncidentAction.CREATE);
        UUID actorId = authorization.userId();
        Incident newIncident = mapper.map(request, Incident.class);
        newIncident.setReportedBy(actorId);
        newIncident.setStatus(IncidentStatus.REPORTED);

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
        audit(savedIncident, actorId, "CREATE", null, savedIncident.getStatus());

        return mapper.map(savedIncident, IncidentResponse.class);
    }

    @Override
    public IncidentResponse getIncidentById(UUID id) {
        Incident incident = findIncidentEntityById(id);
        authorization.requireView(incident);
        return mapper.map(incident, IncidentResponse.class);
    }

    @Override
    public Page<IncidentResponse> getAllIncidents(Pageable pageable) {
        authorization.requireCitizenCreate();
        return repository.findByReportedBy(authorization.userId(), pageable)
                .map(entity -> mapper.map(entity, IncidentResponse.class));
    }

    @Override
    public Page<IncidentResponse> getIncidentsByUser(UUID userId, Pageable pageable) {
        UUID actor = authorization.userId();
        if (!actor.equals(userId)) throw new IncidentAuthorizationException("You are not authorized to perform this action.");
        return repository.findByReportedBy(actor, pageable)
                .map(entity -> mapper.map(entity, IncidentResponse.class));
    }

    @Override
    public Page<IncidentResponse> getIncidentsByStatus(IncidentStatus status, Pageable pageable) {
        authorization.requireCitizenCreate();
        return repository.findByReportedByAndStatus(authorization.userId(), status, pageable)
                .map(entity -> mapper.map(entity, IncidentResponse.class));
    }

    @Override
    public Page<IncidentResponse> getIncidentsBySeverity(Severity severity, Pageable pageable) {
        authorization.requireCitizenCreate();
        return repository.findByReportedByAndSeverity(authorization.userId(), severity, pageable)
                .map(entity -> mapper.map(entity, IncidentResponse.class));
    }

    @Override
    public Page<IncidentResponse> filterIncidents(IncidentStatus status, Severity severity, Pageable pageable) {
        if ("CITIZEN".equals(authorization.role())) {
            UUID actor = authorization.userId();
            if (status != null && severity != null) {
                return repository.findByReportedByAndStatusAndSeverity(actor, status, severity, pageable)
                        .map(entity -> mapper.map(entity, IncidentResponse.class));
            } else if (status != null) {
                return repository.findByReportedByAndStatus(actor, status, pageable).map(entity -> mapper.map(entity, IncidentResponse.class));
            } else if (severity != null) {
                return repository.findByReportedByAndSeverity(actor, severity, pageable).map(entity -> mapper.map(entity, IncidentResponse.class));
            }
            return repository.findByReportedBy(actor, pageable).map(entity -> mapper.map(entity, IncidentResponse.class));
        }
        if ("GOVERNMENT_OFFICIAL".equals(authorization.role())) {
            authorization.requireVerifiedOfficial();
            if (status != null && severity != null)
                return repository.findByStatusAndSeverity(status, severity, pageable).map(entity -> mapper.map(entity, IncidentResponse.class));
            if (status != null)
                return repository.findByStatus(status, pageable).map(entity -> mapper.map(entity, IncidentResponse.class));
            if (severity != null)
                return repository.findBySeverity(severity, pageable).map(entity -> mapper.map(entity, IncidentResponse.class));
            return repository.findAll(pageable).map(entity -> mapper.map(entity, IncidentResponse.class));
        }
        if (!"RESCUE_TEAM".equals(authorization.role()))
            throw new IncidentAuthorizationException("You are not authorized to perform this action.");
        Page<Incident> candidates = repository.findAll(pageable);
        List<IncidentResponse> visible = candidates.getContent().stream()
                .filter(authorization::canView)
                .filter(i -> status == null || i.getStatus() == status)
                .filter(i -> severity == null || i.getSeverity() == severity)
                .map(i -> mapper.map(i, IncidentResponse.class)).toList();
        return new PageImpl<>(visible, pageable, visible.size());
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
    public IncidentResponse updateIncidentStatus(UUID id, IncidentStatus status, MultipartFile resolutionPhoto) {
        Incident incident = findIncidentEntityForUpdate(id);
        authorization.authorize(incident, IncidentAction.VERIFY);
        if (status != IncidentStatus.VERIFIED) {
            throw new BadRequestException("Use the dedicated reject or resolve operation; officials can only verify a reported incident here.");
        }
        if (incident.getStatus() != IncidentStatus.REPORTED) {
            throw new BadRequestException("Only reported incidents can be verified.");
        }
        IncidentStatus previous = incident.getStatus();
        incident.setStatus(IncidentStatus.VERIFIED);
        Incident updatedIncident = repository.save(incident);
        audit(updatedIncident, authorization.userId(), "VERIFY", previous, updatedIncident.getStatus());
        publishIncidentCreatedEvent(updatedIncident);
        return mapper.map(updatedIncident, IncidentResponse.class);
    }

    private void publishIncidentCreatedEvent(Incident incident) {
        IncidentCreatedEvent event = IncidentCreatedEvent.builder()
                .incidentId(incident.getId())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .incidentType(incident.getIncidentType().name())
                .severity(incident.getSeverity().name())
                .latitude(incident.getLatitude())
                .longitude(incident.getLongitude())
                .reportedBy(incident.getReportedBy())
                .createdAt(incident.getCreatedAt())
                .build();
        incidentEventPublisher.publishIncidentCreated(event);
    }

    @Override
    @Transactional
    public IncidentResponse rejectIncident(UUID id, String reason) {
        Incident incident = findIncidentEntityForUpdate(id);
        authorization.authorize(incident, IncidentAction.REJECT);
        if (incident.getStatus() != IncidentStatus.REPORTED) {
            throw new BadRequestException("Only reported incidents can be rejected.");
        }
        String cleanedReason = reason == null ? "" : reason.trim();
        if (cleanedReason.isBlank()) {
            throw new BadRequestException("A reason is required to reject an incident.");
        }
        IncidentStatus previous = incident.getStatus();
        incident.setRejectionReason(cleanedReason);
        incident.setStatus(IncidentStatus.REJECTED);
        Incident saved = repository.save(incident);
        audit(saved, authorization.userId(), "REJECT", previous, saved.getStatus());
        return mapper.map(saved, IncidentResponse.class);
    }

    @Override
    @Transactional
    public IncidentResponse resolveIncident(UUID id, MultipartFile resolutionPhoto, String reason) {
        Incident incident = findIncidentEntityForUpdate(id);
        authorization.authorize(incident, IncidentAction.RESOLVE);
        if (incident.getStatus() != IncidentStatus.DISPATCHED) {
            throw new BadRequestException("Only dispatched incidents can be resolved.");
        }
        String cleanedReason = reason == null ? "" : reason.trim();
        if (cleanedReason.isBlank()) {
            throw new BadRequestException("A resolution reason is required.");
        }
        if (resolutionPhoto == null || resolutionPhoto.isEmpty()) {
            throw new BadRequestException("A resolution photo is required.");
        }
        IncidentStatus previous = incident.getStatus();
        incident.setResolutionPhotoUrl(cloudinaryImageService.upload(resolutionPhoto, "reliefops/resolutions"));
        incident.setResolutionReason(cleanedReason);
        incident.setStatus(IncidentStatus.RESOLVED);
        Incident saved = repository.save(incident);
        audit(saved, authorization.userId(), "RESOLVE", previous, saved.getStatus());
        return mapper.map(saved, IncidentResponse.class);
    }

    @Override
    @Transactional
    public IncidentResponse updateIncident(UUID id, IncidentRequest request, UUID userId) {
        Incident incident = findIncidentEntityById(id);
        authorization.authorize(incident, IncidentAction.UPDATE);

        IncidentStatus previous = incident.getStatus();
        mapper.map(request, incident);

        Incident updatedIncident = repository.save(incident);
        audit(updatedIncident, authorization.userId(), "UPDATE", previous, updatedIncident.getStatus());
        return mapper.map(updatedIncident, IncidentResponse.class);
    }

    @Override
    @Transactional
    public void deleteIncident(UUID id, UUID userId) {
        Incident incident = findIncidentEntityById(id);
        authorization.authorize(incident, IncidentAction.DELETE);
        audit(incident, authorization.userId(), "DELETE", incident.getStatus(), null);
        repository.delete(incident);
    }

    @Override
    @Transactional
    public IncidentResponse assignOfficial(UUID incidentId, AssignOfficialRequest request) {
        authorization.authorize(null, IncidentAction.ASSIGN_OFFICIAL);
        Incident incident = findIncidentEntityForUpdate(incidentId);
        if (!incident.getStatus().isActive()) throw new BadRequestException("Only active incidents can be assigned.");
        IncidentStatus previous = incident.getStatus();
        incident.setAssignedOfficialId(request.getOfficialId());
        Incident updated = repository.save(incident);
        audit(updated, authorization.userId(), "ASSIGN_OFFICIAL", previous, updated.getStatus());
        return mapper.map(updated, IncidentResponse.class);
    }

    @Override
    @Transactional
    public void updateIncidentStatusFromRescue(UUID incidentId, String status, String notes, UUID departmentId, UUID performedBy) {
        Incident incident = findIncidentEntityForUpdate(incidentId);
        IncidentStatus previous = incident.getStatus();
        UUID previousDepartmentId = incident.getAssignedDepartmentId();

        if (incident.getStatus().isTerminal()) {
            log.info("Incident ID: {} is already in terminal state ({}). Skipping status transition from rescue event.",
                    incidentId, incident.getStatus());
            return;
        }

        if ("DISPATCHED".equalsIgnoreCase(status)) {
            if (incident.getStatus() != IncidentStatus.VERIFIED) {
                log.warn("Ignoring dispatch event for incident {} in status {}", incidentId, incident.getStatus());
                return;
            }
            incident.setStatus(IncidentStatus.DISPATCHED);
            if (departmentId != null) incident.setAssignedDepartmentId(departmentId);
        }

        if (notes != null && !notes.isBlank()) {
            String existingDescription = incident.getDescription() != null ? incident.getDescription() : "";
            incident.setDescription(existingDescription + "\n[Rescue Update]: " + notes.trim());
        }

        Incident savedIncident = repository.save(incident);
        if (previous != savedIncident.getStatus() || !java.util.Objects.equals(previousDepartmentId, savedIncident.getAssignedDepartmentId())) {
            audit(savedIncident, performedBy,
                    !java.util.Objects.equals(previousDepartmentId, savedIncident.getAssignedDepartmentId()) ? "ASSIGN_RESCUE_TEAM" : "RESCUE_STATUS",
                    previous, savedIncident.getStatus());
        }
        log.info("Updated Incident ID: {} status to {} based on Rescue Event.", incidentId, savedIncident.getStatus());
    }

    private void audit(Incident incident, UUID actor, String action, IncidentStatus previous, IncidentStatus next) {
        auditRepository.save(new IncidentAudit(incident.getId(), actor, action,
                previous == null ? null : previous.name(), next == null ? null : next.name()));
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

        String existingDescription = incident.getDescription() != null ? incident.getDescription() : "";
        String noteSummary = String.format("\n[Rescue mission %s completed; awaiting official resolution]: %s",
                missionId, resolutionNotes != null ? resolutionNotes : "Completed successfully.");
        incident.setDescription(existingDescription + noteSummary);
        repository.save(incident);
        log.info("Rescue mission completion recorded for incident {} without changing its resolution status.", incidentId);
    }

    private Incident findIncidentEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident is not found with id " + id));
    }

    private Incident findIncidentEntityForUpdate(UUID id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident is not found with id " + id));
    }

    @Override
    public void notifyEndUser(UUID incidentId, String status, String notes) {
        log.info("Notifying end-user for Incident ID: {} with new Status: {} and Notes: {}",
                incidentId, status, notes);
    }
}
