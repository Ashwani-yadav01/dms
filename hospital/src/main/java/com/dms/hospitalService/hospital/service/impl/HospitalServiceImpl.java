package com.dms.hospitalService.hospital.service.impl;

import com.dms.hospitalService.hospital.dto.request.HospitalCreateRequest;
import com.dms.hospitalService.hospital.dto.request.HospitalUpdateRequest;
import com.dms.hospitalService.hospital.dto.response.HospitalResponse;
import com.dms.hospitalService.hospital.entity.Hospital;
import com.dms.hospitalService.hospital.entity.HospitalStatus;
import com.dms.hospitalService.hospital.entity.MedicalSpeciality;
import com.dms.hospitalService.hospital.repository.HospitalRepository;
import com.dms.hospitalService.hospital.service.HospitalRedisGeoService;
import com.dms.hospitalService.hospital.service.HospitalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalServiceImpl implements HospitalService {

    private final HospitalRepository hospitalRepository;
    private final HospitalRedisGeoService geoService;

    @Override
    @Transactional
    public HospitalResponse createHospital(HospitalCreateRequest request) {
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
                .build();

        hospital = hospitalRepository.save(hospital);

        // Sync with Redis GEO immediately
        geoService.registerHospitalLocation(hospital.getId(), hospital.getLatitude(), hospital.getLongitude());

        log.info("Created new hospital: {}", hospital.getName());
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
        hospital.setIsAcceptingPatients(status);
        return mapToResponse(hospitalRepository.save(hospital));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HospitalResponse> findNearestCapableHospitals(
            Double latitude, Double longitude, Double radiusKm,
            Set<MedicalSpeciality> requiredSpecialities, Boolean requiresIcu) {

        // 1. Get ordered list of nearest hospital IDs from Redis
        List<UUID> nearestIds = geoService.findHospitalsWithinRadius(latitude, longitude, radiusKm);
        if (nearestIds.isEmpty()) return Collections.emptyList();

        // 2. Fetch from DB
        List<Hospital> hospitals = hospitalRepository.findByIdInAndIsAcceptingPatientsTrue(nearestIds);

        // 3. Filter by capabilities & capacity, then sort back to Redis distance order
        return hospitals.stream()
                .filter(h -> !requiresIcu || h.getAvailableIcuBeds() > 0)
                .filter(h -> (!requiresIcu && h.getAvailableGeneralBeds() > 0) || requiresIcu)
                .filter(h -> requiredSpecialities == null || requiredSpecialities.isEmpty() || h.getSpecialities().containsAll(requiredSpecialities))
                // Re-sort based on the distance order returned by Redis
                .sorted(Comparator.comparingInt(h -> nearestIds.indexOf(h.getId())))
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
                .status(HospitalStatus.ACTIVE)
                .build();

        hospital = hospitalRepository.save(hospital);

        // Sync with Redis GEO immediately
        geoService.registerHospitalLocation(hospital.getId(), hospital.getLatitude(), hospital.getLongitude());

        log.info("Admin created new hospital: {}", hospital.getName());
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
        if (request.getLatitude() != null) hospital.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) hospital.setLongitude(request.getLongitude());
        if (request.getElevationMeters() != null) hospital.setElevationMeters(request.getElevationMeters());
        if (request.getTotalGeneralBeds() != null) hospital.setTotalGeneralBeds(request.getTotalGeneralBeds());
        if (request.getTotalIcuBeds() != null) hospital.setTotalIcuBeds(request.getTotalIcuBeds());
        if (request.getSpecialities() != null) hospital.setSpecialities(request.getSpecialities());
        if (request.getIsAcceptingPatients() != null) hospital.setIsAcceptingPatients(request.getIsAcceptingPatients());

        // If coordinates changed, update Redis GEO
        if (request.getLatitude() != null || request.getLongitude() != null) {
            geoService.registerHospitalLocation(hospital.getId(), hospital.getLatitude(), hospital.getLongitude());
        }

        hospital = hospitalRepository.save(hospital);
        log.info("Admin updated hospital: {}", hospital.getName());
        return mapToResponse(hospital);
    }

    @Override
    @Transactional
    public HospitalResponse changeHospitalStatus(UUID id, HospitalStatus newStatus) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found: " + id));

        HospitalStatus oldStatus = hospital.getStatus();
        if (oldStatus == newStatus) {
            log.info("Hospital {} already has status {}", id, newStatus);
            return mapToResponse(hospital);
        }

        hospital.setStatus(newStatus);
        hospital = hospitalRepository.save(hospital);

        // Sync Redis GEO based on new status
        if (newStatus == HospitalStatus.ACTIVE) {
            // Re-register in geo index for routing
            geoService.registerHospitalLocation(hospital.getId(), hospital.getLatitude(), hospital.getLongitude());
            log.info("Hospital {} reactivated and added to geo index", id);
        } else {
            // Remove from geo index for SUSPENDED/DEACTIVATED
            geoService.removeHospitalLocation(id);
            log.info("Hospital {} status changed to {} and removed from geo index", id, newStatus);
        }

        return mapToResponse(hospital);
    }
}
