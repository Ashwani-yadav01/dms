package com.dms.hospitalService.hospital.config;

import com.dms.hospitalService.hospital.entity.FacilityType;
import com.dms.hospitalService.hospital.entity.Hospital;
import com.dms.hospitalService.hospital.entity.MedicalSpeciality;
import com.dms.hospitalService.hospital.repository.HospitalRepository;
import com.dms.hospitalService.hospital.service.HospitalRedisGeoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Idempotent local-development geography fixtures; records use the normal hospital table and GEO index. */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class LocationTestDataInitializer implements CommandLineRunner {
    private final HospitalRepository hospitals;
    private final HospitalRedisGeoService geo;

    // Gorakhpur city center. Relative distances cover the service's 50 km hospital search,
    // its 15 km incident standby event radius, and a case beyond both.
    private static final double CENTER_LAT = 26.7606;
    private static final double CENTER_LON = 83.3732;

    @Override
    public void run(String... args) {
        seed("DMS Test - BRD Medical College", "BRD Medical College & Hospital, Medical College Road", 26.8133, 83.4025,
                FacilityType.TRAUMA_CENTER, 120, 4, 80, 2, true,
                Set.of(MedicalSpeciality.LEVEL_1_TRAUMA, MedicalSpeciality.CARDIAC_ICU));
        seed("DMS Test - City General Hospital", "Golghar, Gorakhpur", 26.7580, 83.3730,
                FacilityType.GENERAL_HOSPITAL, 80, 20, 12, 3, true,
                Set.of(MedicalSpeciality.CARDIAC_ICU, MedicalSpeciality.NEONATAL_ICU));
        seed("DMS Test - Railway Hospital", "Railway Colony, Gorakhpur", 26.7480, 83.3810,
                FacilityType.GENERAL_HOSPITAL, 45, 0, 4, 0, true,
                Set.of(MedicalSpeciality.LEVEL_1_TRAUMA));
        seed("DMS Test - Gorakhnath Community Hospital", "Gorakhnath, Gorakhpur", 26.7900, 83.3900,
                FacilityType.GENERAL_HOSPITAL, 60, 12, 8, 1, true,
                Set.of(MedicalSpeciality.NEONATAL_ICU));
        seed("DMS Test - Rustampur Trauma Centre", "Rustampur, Gorakhpur", 26.7300, 83.3600,
                FacilityType.TRAUMA_CENTER, 70, 18, 10, 2, true,
                Set.of(MedicalSpeciality.LEVEL_1_TRAUMA, MedicalSpeciality.BURN_UNIT));
        seed("DMS Test - Chargawan PHC", "Chargawan, Gorakhpur", 26.8300, 83.4300,
                FacilityType.PRIMARY_HEALTH_CENTRE, 25, 6, 2, 1, true,
                Set.of(MedicalSpeciality.ISOLATION_WARD));
        seed("DMS Test - Pipiganj Hospital", "Pipiganj, Gorakhpur district", 26.9100, 83.3300,
                FacilityType.GENERAL_HOSPITAL, 50, 15, 6, 2, true,
                Set.of(MedicalSpeciality.CARDIAC_ICU));
        seed("DMS Test - Sahjanwa Hospital", "Sahjanwa, Gorakhpur district", 26.7500, 83.2100,
                FacilityType.GENERAL_HOSPITAL, 55, 12, 6, 1, true,
                Set.of(MedicalSpeciality.LEVEL_1_TRAUMA));
        seed("DMS Test - Bansgaon Field Hospital", "Bansgaon, Gorakhpur district", 26.5500, 83.3500,
                FacilityType.FIELD_HOSPITAL, 30, 10, 2, 1, true,
                Set.of(MedicalSpeciality.BURN_UNIT));
        seed("DMS Test - Deoria Road Hospital", "Kauriram, Gorakhpur district", 26.5700, 83.5700,
                FacilityType.GENERAL_HOSPITAL, 35, 8, 2, 0, true,
                Set.of(MedicalSpeciality.ISOLATION_WARD));
        seed("DMS Test - Deoria District Hospital", "Deoria city", 26.5047, 83.7799,
                FacilityType.GENERAL_HOSPITAL, 90, 24, 12, 3, true,
                Set.of(MedicalSpeciality.LEVEL_1_TRAUMA, MedicalSpeciality.CARDIAC_ICU));
        seed("DMS Test - Salempur Hospital", "Salempur, Deoria district", 26.3000, 83.9200,
                FacilityType.GENERAL_HOSPITAL, 40, 12, 4, 1, true,
                Set.of(MedicalSpeciality.NEONATAL_ICU));
        seed("DMS Test - Maharajganj District Hospital", "Maharajganj", 27.1447, 83.5625,
                FacilityType.GENERAL_HOSPITAL, 65, 18, 8, 2, true,
                Set.of(MedicalSpeciality.LEVEL_1_TRAUMA));
        seed("DMS Test - Closed City Clinic", "Taramandal, Gorakhpur", 26.7350, 83.4000,
                FacilityType.CLINIC, 20, 15, 0, 0, false, Set.of());
        log.info("Location fixtures ready: {} DMS test hospitals", 14);
    }

    private void seed(String name, String address, double lat, double lon, FacilityType type,
                      int totalGeneral, int availableGeneral, int totalIcu, int availableIcu,
                      boolean accepting, Set<MedicalSpeciality> specialities) {
        Hospital hospital = hospitals.findByName(name).orElseGet(() -> hospitals.save(Hospital.builder()
                .name(name)
                .email("dms-test+" + name.toLowerCase().replaceAll("[^a-z0-9]+", "") + "@example.org")
                .type(type)
                .latitude(lat)
                .longitude(lon)
                .elevationMeters(95.0)
                .totalGeneralBeds(totalGeneral)
                .availableGeneralBeds(availableGeneral)
                .totalIcuBeds(totalIcu)
                .availableIcuBeds(availableIcu)
                .specialities(specialities)
                .isAcceptingPatients(accepting)
                .build()));
        // Repair the spatial index on every boot, including after Redis data is cleared.
        geo.registerHospitalLocation(hospital.getId(), hospital.getLatitude(), hospital.getLongitude());
    }
}
