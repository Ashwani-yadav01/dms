package com.dms.hospitalService.hospital.service.impl;

import com.dms.hospitalService.hospital.dto.request.HospitalBedUpdateRequest;
import com.dms.hospitalService.hospital.dto.request.HospitalCreateRequest;
import com.dms.hospitalService.hospital.dto.request.HospitalUpdateRequest;
import com.dms.hospitalService.hospital.dto.response.HospitalResponse;
import com.dms.hospitalService.hospital.entity.Hospital;
import com.dms.hospitalService.hospital.entity.HospitalAudit;
import com.dms.hospitalService.hospital.entity.HospitalStatus;
import com.dms.hospitalService.hospital.entity.MedicalSpeciality;
import com.dms.hospitalService.hospital.repository.HospitalAuditRepository;
import com.dms.hospitalService.hospital.repository.HospitalRepository;
import com.dms.hospitalService.hospital.service.HospitalRedisGeoService;
import com.dms.hospitalService.hospital.service.HospitalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalServiceImpl implements HospitalService {

    private final HospitalRepository hospitalRepository;
    private final HospitalRedisGeoService geoService;
    private final HospitalAuditRepository auditRepository;

    private void recordAudit(UUID hospitalId, UUID performedBy, String actorRole, String action, String details, String prevValue, String newValue) {
        try {
            HospitalAudit audit = HospitalAudit.builder()
                    .hospitalId(hospitalId)
                    .performedBy(performedBy)
                    .actorRole(actorRole != null ? actorRole : "SYSTEM")
                    .action(action)
                    .details(details)
                    .previousValue(prevValue)
                    .newValue(newValue)
                    .occurredAt(LocalDateTime.now())
                    .build();
            auditRepository.save(audit);
        } catch (Exception e) {
            log.warn("Failed to persist hospital audit log: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public HospitalResponse createHospital(HospitalCreateRequest request) {
        HospitalStatus initialStatus = request.getStatus() != null ? request.getStatus() : HospitalStatus.PENDING_VERIFICATION;
        Hospital hospital = Hospital.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .type(request.getType())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .elevationMeters(request.getElevationMeters())
                .totalGeneralBeds(request.getTotalGeneralBeds())
                .availableGeneralBeds(request.getTotalGeneralBeds())
                .totalIcuBeds(request.getTotalIcuBeds())
                .availableIcuBeds(request.getTotalIcuBeds())
                .specialities(request.getSpecialities() != null ? request.getSpecialities() : new HashSet<>())
                .isAcceptingPatients(true)
                .status(initialStatus)
                .adminUserId(request.getAdminUserId())
                .locationLastUpdatedAt(LocalDateTime.now())
                .bedLastUpdatedAt(LocalDateTime.now())
                .emergencyStatusLastUpdatedAt(LocalDateTime.now())
                .build();

        hospital = hospitalRepository.save(hospital);

        // Only register in Redis GEO if ACTIVE (not when PENDING_VERIFICATION or REJECTED)
        if (initialStatus == HospitalStatus.ACTIVE) {
            geoService.registerHospitalLocation(hospital.getId(), hospital.getLatitude(), hospital.getLongitude());
        }

        recordAudit(hospital.getId(), request.getAdminUserId(), "HOSPITAL", "CREATE", "Hospital registered", null, initialStatus.name());
        log.info("Created new hospital: {} with initial status: {}", hospital.getName(), initialStatus);
        return mapToResponse(hospital);
    }

    @Override
    public HospitalResponse getHospitalById(UUID id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));
        return mapToResponse(hospital);
    }

    @Override
    @Transactional
    public HospitalResponse toggleAcceptingPatients(UUID id, Boolean status) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));
        Boolean oldStatus = hospital.getIsAcceptingPatients();
        hospital.setIsAcceptingPatients(status);
        hospital.setEmergencyStatusLastUpdatedAt(LocalDateTime.now());
        hospital = hospitalRepository.save(hospital);
        recordAudit(hospital.getId(), null, "STAFF", "INTAKE_TOGGLE",
                "Emergency intake accepting status toggled to " + status,
                String.valueOf(oldStatus), String.valueOf(status));
        return mapToResponse(hospital);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HospitalResponse> findNearestCapableHospitals(
            Double latitude, Double longitude, Double radiusKm,
            Set<MedicalSpeciality> requiredSpecialities, Boolean requiresIcu) {

        // 1. Get ordered list of nearest hospital IDs from Redis
        List<UUID> nearestIds = geoService.findHospitalsWithinRadius(latitude, longitude, radiusKm);
        List<Hospital> hospitals;

        if (nearestIds.isEmpty()) {
            // Self-healing fallback: query PostgreSQL active hospitals within radius and backfill Redis GEO
            List<Hospital> activeHospitals = hospitalRepository.findByStatusAndIsAcceptingPatientsTrue(HospitalStatus.ACTIVE);
            hospitals = activeHospitals.stream()
                    .filter(h -> distanceKm(latitude, longitude, h.getLatitude(), h.getLongitude()) <= radiusKm)
                    .peek(h -> {
                        try {
                            geoService.registerHospitalLocation(h.getId(), h.getLatitude(), h.getLongitude());
                        } catch (Exception ignored) {}
                    })
                    .sorted(Comparator.comparingDouble(h -> distanceKm(latitude, longitude, h.getLatitude(), h.getLongitude())))
                    .collect(Collectors.toList());
        } else {
            // 2. Fetch from DB using Redis result order
            hospitals = hospitalRepository.findByIdInAndIsAcceptingPatientsTrue(nearestIds);
        }

        // 3. Filter by capabilities & capacity, then sort back to distance order
        return hospitals.stream()
                .filter(h -> !requiresIcu || h.getAvailableIcuBeds() > 0)
                .filter(h -> (!requiresIcu && h.getAvailableGeneralBeds() > 0) || requiresIcu)
                .filter(h -> requiredSpecialities == null || requiredSpecialities.isEmpty() || h.getSpecialities().containsAll(requiredSpecialities))
                .sorted(Comparator.comparingDouble(h -> distanceKm(latitude, longitude, h.getLatitude(), h.getLongitude())))
                .map(h -> mapToResponse(h, distanceKm(latitude, longitude, h.getLatitude(), h.getLongitude())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteHospital(UUID id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));

        // 1. Remove from Redis GEO Cache immediately to stop incoming routing!
        geoService.removeHospitalLocation(id);

        // 2. Delete from PostgreSQL (JPA Cascade will handle Inventory & Admissions)
        hospitalRepository.delete(hospital);

        log.info("Successfully deleted hospital and associated records for ID: {}", id);
    }

    private HospitalResponse mapToResponse(Hospital h) {
        return mapToResponse(h, null);
    }

    private HospitalResponse mapToResponse(Hospital h, Double distanceKm) {
        return HospitalResponse.builder()
                .id(h.getId())
                .name(h.getName())
                .email(h.getEmail())
                .phoneNumber(h.getPhoneNumber())
                .address(h.getAddress())
                .distanceKm(distanceKm)
                .type(h.getType())
                .latitude(h.getLatitude())
                .longitude(h.getLongitude())
                .elevationMeters(h.getElevationMeters())
                .availableGeneralBeds(h.getAvailableGeneralBeds())
                .availableIcuBeds(h.getAvailableIcuBeds())
                .specialities(h.getSpecialities())
                .isAcceptingPatients(h.getIsAcceptingPatients())
                .status(h.getStatus())
                .statusReason(h.getStatusReason())
                .adminUserId(h.getAdminUserId())
                .verifiedBy(h.getVerifiedBy())
                .verifiedAt(h.getVerifiedAt())
                .locationLastUpdatedAt(h.getLocationLastUpdatedAt())
                .bedLastUpdatedAt(h.getBedLastUpdatedAt())
                .emergencyStatusLastUpdatedAt(h.getEmergencyStatusLastUpdatedAt())
                .updatedAt(h.getUpdatedAt())
                .build();
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0088;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HospitalResponse> listHospitals(Pageable pageable, HospitalStatus status) {
        Page<Hospital> page = (status != null)
                ? hospitalRepository.findByStatus(status, pageable)
                : hospitalRepository.findAll(pageable);
        return page.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public HospitalResponse createHospitalAdmin(HospitalCreateRequest request) {
        // Check for duplicate by email
        if (hospitalRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Hospital with this email already exists: " + request.getEmail());
        }
        // Check for duplicate by name
        if (hospitalRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Hospital with this name already exists: " + request.getName());
        }

        HospitalStatus initialStatus = request.getStatus() != null ? request.getStatus() : HospitalStatus.ACTIVE;
        Hospital hospital = Hospital.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .type(request.getType())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .elevationMeters(request.getElevationMeters())
                .totalGeneralBeds(request.getTotalGeneralBeds())
                .availableGeneralBeds(request.getTotalGeneralBeds())
                .totalIcuBeds(request.getTotalIcuBeds())
                .availableIcuBeds(request.getTotalIcuBeds())
                .specialities(request.getSpecialities() != null ? request.getSpecialities() : new HashSet<>())
                .isAcceptingPatients(true)
                .status(initialStatus)
                .adminUserId(request.getAdminUserId())
                .locationLastUpdatedAt(LocalDateTime.now())
                .bedLastUpdatedAt(LocalDateTime.now())
                .emergencyStatusLastUpdatedAt(LocalDateTime.now())
                .build();

        hospital = hospitalRepository.save(hospital);

        if (initialStatus == HospitalStatus.ACTIVE) {
            geoService.registerHospitalLocation(hospital.getId(), hospital.getLatitude(), hospital.getLongitude());
        }

        recordAudit(hospital.getId(), request.getAdminUserId(), "ADMIN", "CREATE_ADMIN", "Hospital created by administrator", null, initialStatus.name());
        log.info("Admin created new hospital: {} with status: {}", hospital.getName(), initialStatus);
        return mapToResponse(hospital);
    }

    @Override
    @Transactional
    public HospitalResponse updateHospital(UUID id, HospitalUpdateRequest request) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found: " + id));

        // Check for duplicate email if changing
        if (request.getEmail() != null && !request.getEmail().equals(hospital.getEmail())) {
            if (hospitalRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Hospital with this email already exists: " + request.getEmail());
            }
            hospital.setEmail(request.getEmail());
        }

        // Check for duplicate name if changing
        if (request.getName() != null && !request.getName().equals(hospital.getName())) {
            if (hospitalRepository.findByName(request.getName()).isPresent()) {
                throw new IllegalArgumentException("Hospital with this name already exists: " + request.getName());
            }
            hospital.setName(request.getName());
        }

        if (request.getPhoneNumber() != null) hospital.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddress() != null) hospital.setAddress(request.getAddress());
        if (request.getType() != null) hospital.setType(request.getType());
        if (request.getElevationMeters() != null) hospital.setElevationMeters(request.getElevationMeters());
        if (request.getTotalGeneralBeds() != null) hospital.setTotalGeneralBeds(request.getTotalGeneralBeds());
        if (request.getTotalIcuBeds() != null) hospital.setTotalIcuBeds(request.getTotalIcuBeds());
        if (request.getSpecialities() != null) hospital.setSpecialities(request.getSpecialities());
        if (request.getIsAcceptingPatients() != null) hospital.setIsAcceptingPatients(request.getIsAcceptingPatients());
        if (request.getAdminUserId() != null) hospital.setAdminUserId(request.getAdminUserId());
        if (request.getStatusReason() != null) hospital.setStatusReason(request.getStatusReason());

        boolean coordsChanged = false;
        double oldLat = hospital.getLatitude();
        double oldLon = hospital.getLongitude();

        if (request.getLatitude() != null && !request.getLatitude().equals(hospital.getLatitude())) {
            hospital.setLatitude(request.getLatitude());
            coordsChanged = true;
        }
        if (request.getLongitude() != null && !request.getLongitude().equals(hospital.getLongitude())) {
            hospital.setLongitude(request.getLongitude());
            coordsChanged = true;
        }

        if (coordsChanged) {
            hospital.setLocationLastUpdatedAt(LocalDateTime.now());
            if (hospital.getStatus() == HospitalStatus.ACTIVE) {
                geoService.registerHospitalLocation(hospital.getId(), hospital.getLatitude(), hospital.getLongitude());
            }
            recordAudit(hospital.getId(), null, "ADMIN", "LOCATION_UPDATE", "Coordinates updated",
                    oldLat + "," + oldLon, hospital.getLatitude() + "," + hospital.getLongitude());
        }

        hospital = hospitalRepository.save(hospital);
        recordAudit(hospital.getId(), null, "ADMIN", "UPDATE", "Hospital attributes updated", null, null);
        log.info("Admin updated hospital: {}", hospital.getName());
        return mapToResponse(hospital);
    }

    @Override
    @Transactional
    public HospitalResponse changeHospitalStatus(UUID id, HospitalStatus newStatus) {
        return changeHospitalStatus(id, newStatus, null, null, "ADMIN");
    }

    @Override
    @Transactional
    public HospitalResponse changeHospitalStatus(UUID id, HospitalStatus newStatus, String reason, UUID actorId, String actorRole) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found: " + id));

        HospitalStatus oldStatus = hospital.getStatus();
        if (oldStatus == newStatus) {
            log.info("Hospital {} already has status {}", id, newStatus);
            return mapToResponse(hospital);
        }

        hospital.setStatus(newStatus);
        if (reason != null && !reason.isBlank()) {
            hospital.setStatusReason(reason.trim());
        }
        hospital.setEmergencyStatusLastUpdatedAt(LocalDateTime.now());
        hospital = hospitalRepository.save(hospital);

        // Sync Redis GEO based on new status
        if (newStatus == HospitalStatus.ACTIVE) {
            // Re-register in geo index for routing
            geoService.registerHospitalLocation(hospital.getId(), hospital.getLatitude(), hospital.getLongitude());
            log.info("Hospital {} reactivated and added to geo index", id);
        } else {
            // Remove from geo index for SUSPENDED/DEACTIVATED/REJECTED
            geoService.removeHospitalLocation(id);
            log.info("Hospital {} status changed to {} and removed from geo index", id, newStatus);
        }

        recordAudit(hospital.getId(), actorId, actorRole, "STATUS_CHANGE",
                reason != null ? "Status change reason: " + reason : "Status changed",
                oldStatus.name(), newStatus.name());

        return mapToResponse(hospital);
    }

    @Override
    @Transactional
    public HospitalResponse verifyHospital(UUID id, UUID actorId, String actorRole) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found: " + id));

        HospitalStatus oldStatus = hospital.getStatus();
        hospital.setStatus(HospitalStatus.ACTIVE);
        hospital.setVerifiedBy(actorId);
        hospital.setVerifiedAt(LocalDateTime.now());
        hospital.setStatusReason(null);
        hospital.setEmergencyStatusLastUpdatedAt(LocalDateTime.now());
        hospital = hospitalRepository.save(hospital);

        geoService.registerHospitalLocation(hospital.getId(), hospital.getLatitude(), hospital.getLongitude());
        recordAudit(hospital.getId(), actorId, actorRole, "VERIFY", "Hospital verified and activated",
                oldStatus != null ? oldStatus.name() : "PENDING_VERIFICATION", HospitalStatus.ACTIVE.name());

        log.info("Hospital {} officially verified by {}", id, actorId);
        return mapToResponse(hospital);
    }

    @Override
    @Transactional
    public HospitalResponse rejectHospital(UUID id, String reason, UUID actorId, String actorRole) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found: " + id));

        HospitalStatus oldStatus = hospital.getStatus();
        hospital.setStatus(HospitalStatus.REJECTED);
        hospital.setVerifiedBy(actorId);
        hospital.setVerifiedAt(LocalDateTime.now());
        hospital.setStatusReason(reason != null ? reason.trim() : "Rejected by authority");
        hospital.setEmergencyStatusLastUpdatedAt(LocalDateTime.now());
        hospital = hospitalRepository.save(hospital);

        geoService.removeHospitalLocation(id);
        recordAudit(hospital.getId(), actorId, actorRole, "REJECT", "Hospital registration rejected: " + reason,
                oldStatus != null ? oldStatus.name() : "PENDING_VERIFICATION", HospitalStatus.REJECTED.name());

        log.info("Hospital {} rejected by {}: {}", id, actorId, reason);
        return mapToResponse(hospital);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HospitalAudit> getHospitalAudits(UUID hospitalId) {
        return auditRepository.findByHospitalIdOrderByOccurredAtDesc(hospitalId);
    }

    @Override
    @Transactional
    public HospitalResponse updateBedCapacity(UUID id, HospitalBedUpdateRequest request) {
        Hospital hospital = hospitalRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found: " + id));

        if (request.getAvailableGeneralBeds() > hospital.getTotalGeneralBeds()) {
            throw new IllegalArgumentException("Available general beds (" + request.getAvailableGeneralBeds()
                    + ") cannot exceed total general beds (" + hospital.getTotalGeneralBeds() + ")");
        }
        if (request.getAvailableIcuBeds() > hospital.getTotalIcuBeds()) {
            throw new IllegalArgumentException("Available ICU beds (" + request.getAvailableIcuBeds()
                    + ") cannot exceed total ICU beds (" + hospital.getTotalIcuBeds() + ")");
        }

        int prevGen = hospital.getAvailableGeneralBeds();
        int prevIcu = hospital.getAvailableIcuBeds();

        hospital.setAvailableGeneralBeds(request.getAvailableGeneralBeds());
        hospital.setAvailableIcuBeds(request.getAvailableIcuBeds());
        hospital.setBedLastUpdatedAt(LocalDateTime.now());

        hospital = hospitalRepository.save(hospital);

        recordAudit(hospital.getId(), null, "STAFF", "BED_UPDATE",
                "Bed capacity updated",
                "general=" + prevGen + ",icu=" + prevIcu,
                "general=" + request.getAvailableGeneralBeds() + ",icu=" + request.getAvailableIcuBeds());

        log.info("Updated bed capacity for hospital {}: general={}, icu={}",
                hospital.getName(), request.getAvailableGeneralBeds(), request.getAvailableIcuBeds());
        return mapToResponse(hospital);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HospitalResponse> searchHospitalsByName(String name, Pageable pageable) {
        Page<Hospital> page = hospitalRepository.findByNameContainingIgnoreCaseAndStatus(
                name, HospitalStatus.ACTIVE, pageable);
        return page.map(this::mapToResponse);
    }
}
