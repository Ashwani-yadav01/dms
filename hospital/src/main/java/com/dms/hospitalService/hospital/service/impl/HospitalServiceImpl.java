package com.dms.hospitalService.hospital.service.impl;

import com.dms.hospitalService.hospital.dto.request.HospitalCreateRequest;
import com.dms.hospitalService.hospital.dto.response.HospitalResponse;
import com.dms.hospitalService.hospital.entity.Hospital;
import com.dms.hospitalService.hospital.entity.MedicalSpeciality;
import com.dms.hospitalService.hospital.repository.HospitalRepository;
import com.dms.hospitalService.hospital.service.HospitalRedisGeoService;
import com.dms.hospitalService.hospital.service.HospitalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                .map(this::mapToResponse)
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
        return HospitalResponse.builder()
                .id(h.getId())
                .name(h.getName())
                .type(h.getType())
                .latitude(h.getLatitude())
                .longitude(h.getLongitude())
                .elevationMeters(h.getElevationMeters())
                .availableGeneralBeds(h.getAvailableGeneralBeds())
                .availableIcuBeds(h.getAvailableIcuBeds())
                .specialities(h.getSpecialities())
                .isAcceptingPatients(h.getIsAcceptingPatients())
                .updatedAt(h.getUpdatedAt())
                .build();
    }
}