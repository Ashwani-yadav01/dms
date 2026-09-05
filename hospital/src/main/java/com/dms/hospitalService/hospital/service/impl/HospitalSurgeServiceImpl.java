package com.dms.hospitalService.hospital.service.impl;

import com.dms.hospitalService.hospital.entity.Hospital;
import com.dms.hospitalService.hospital.kafka.event.HospitalSurgeStandbyEvent;
import com.dms.hospitalService.hospital.kafka.producer.HospitalKafkaProducer;
import com.dms.hospitalService.hospital.repository.HospitalRepository;
import com.dms.hospitalService.hospital.service.HospitalRedisGeoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalSurgeServiceImpl {

    private final HospitalRedisGeoService geoService;
    private final HospitalRepository hospitalRepository;
    private final HospitalKafkaProducer kafkaProducer;

    /**
     * Finds all nearby accepting hospitals within danger radius using Redis GEO,
     * and alerts each of them to stand by for incoming casualties.
     */
    @Transactional(readOnly = true)
    public void alertNearbyHospitalsForIncident(
            UUID incidentId,
            String disasterType,
            double incidentLatitude,
            double incidentLongitude,
            double radiusKm,
            int estimatedCasualties) {

        log.info("Searching for hospitals within {} km of Incident {} [{}, {}] to trigger surge standby",
                radiusKm, incidentId, incidentLatitude, incidentLongitude);

        // 1. Get ordered hospital IDs using your existing Redis GEO Service
        List<UUID> hospitalIds = geoService.findHospitalsWithinRadius(incidentLatitude, incidentLongitude, radiusKm);

        if (hospitalIds.isEmpty()) {
            log.warn("No hospitals found within {} km of incident {}", radiusKm, incidentId);
            return;
        }

        // 2. Fetch accepting hospitals from DB
        List<Hospital> hospitals = hospitalRepository.findByIdInAndIsAcceptingPatientsTrue(hospitalIds);

        // 3. For each hospital, calculate distance and publish event to Kafka
        for (Hospital hospital : hospitals) {
            double distance = calculateDistanceKm(
                    incidentLatitude, incidentLongitude,
                    hospital.getLatitude(), hospital.getLongitude()
            );

            HospitalSurgeStandbyEvent surgeEvent = HospitalSurgeStandbyEvent.builder()
                    .incidentId(incidentId)
                    .hospitalEmail(hospital.getEmail() != null ? hospital.getEmail() : "admin@" + hospital.getName().toLowerCase().replaceAll("\\s+", "") + ".org")
                    .hospitalName(hospital.getName())
                    .disasterType(disasterType)
                    .distanceKm(Math.round(distance * 100.0) / 100.0)
                    .estimatedCasualties(estimatedCasualties)
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaProducer.sendHospitalSurgeStandbyAlert(surgeEvent);
        }

        log.info("Dispatched surge standby alerts to {} nearby hospitals", hospitals.size());
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}