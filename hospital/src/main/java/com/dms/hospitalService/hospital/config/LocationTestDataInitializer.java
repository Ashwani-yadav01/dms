package com.dms.hospitalService.hospital.config;

import com.dms.hospitalService.hospital.entity.FacilityType;
import com.dms.hospitalService.hospital.entity.Hospital;
import com.dms.hospitalService.hospital.entity.HospitalStatus;
import com.dms.hospitalService.hospital.entity.MedicalSpeciality;
import com.dms.hospitalService.hospital.repository.HospitalRepository;
import com.dms.hospitalService.hospital.service.HospitalRedisGeoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Idempotent local & test environment geography fixtures for Sahjanwa & Gorakhpur region.
 * Provisions exactly 5 active hospitals with linked administrator accounts.
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class LocationTestDataInitializer implements CommandLineRunner {

    private final HospitalRepository hospitals;
    private final HospitalRedisGeoService geo;

    public static final UUID SAHJANWA_ADMIN_ID   = UUID.fromString("40000000-0000-0000-0000-000000000001");
    public static final UUID BRD_ADMIN_ID        = UUID.fromString("40000000-0000-0000-0000-000000000002");
    public static final UUID CITY_ADMIN_ID       = UUID.fromString("40000000-0000-0000-0000-000000000003");
    public static final UUID RUSTAMPUR_ADMIN_ID   = UUID.fromString("40000000-0000-0000-0000-000000000004");
    public static final UUID GORAKHNATH_ADMIN_ID = UUID.fromString("40000000-0000-0000-0000-000000000005");

    private static final Map<String, HospitalAdminBinding> ADMIN_BINDINGS = Map.of(
            "DMS Test - Sahjanwa Hospital", new HospitalAdminBinding(SAHJANWA_ADMIN_ID, "hospital.sahajanwa@dms.gov.in"),
            "DMS Test - BRD Medical College", new HospitalAdminBinding(BRD_ADMIN_ID, "hospital.brd@dms.gov.in"),
            "DMS Test - City General Hospital", new HospitalAdminBinding(CITY_ADMIN_ID, "hospital.city@dms.gov.in"),
            "DMS Test - Rustampur Trauma Centre", new HospitalAdminBinding(RUSTAMPUR_ADMIN_ID, "hospital.rustampur@dms.gov.in"),
            "DMS Test - Gorakhnath Community Hospital", new HospitalAdminBinding(GORAKHNATH_ADMIN_ID, "hospital.gorakhnath@dms.gov.in")
    );

    private record HospitalAdminBinding(UUID adminUserId, String email) {}

    @Override
    public void run(String... args) {
        log.info("Bootstrapping 5 DMS test hospitals around Sahjanwa and Gorakhpur...");

        // 1. Sahjanwa Hospital (Main facility in Sahajanwa)
        seed("DMS Test - Sahjanwa Hospital", "NH-28 Main Road, Sahajanwa, Gorakhpur", 26.7500, 83.2100,
                FacilityType.GENERAL_HOSPITAL, 60, 15, 10, 4, true,
                Set.of(MedicalSpeciality.LEVEL_1_TRAUMA, MedicalSpeciality.CARDIAC_ICU));

        // 2. BRD Medical College (Tertiary Trauma Center)
        seed("DMS Test - BRD Medical College", "BRD Medical College & Hospital, Medical College Road, Gorakhpur", 26.8133, 83.4025,
                FacilityType.TRAUMA_CENTER, 150, 40, 50, 12, true,
                Set.of(MedicalSpeciality.LEVEL_1_TRAUMA, MedicalSpeciality.CARDIAC_ICU, MedicalSpeciality.BURN_UNIT));

        // 3. City General Hospital (Gorakhpur Center)
        seed("DMS Test - City General Hospital", "Civil Lines, Golghar, Gorakhpur", 26.7580, 83.3730,
                FacilityType.GENERAL_HOSPITAL, 80, 20, 15, 5, true,
                Set.of(MedicalSpeciality.CARDIAC_ICU, MedicalSpeciality.NEONATAL_ICU));

        // 4. Rustampur Trauma Centre (Rapti Bridge Corridor towards Sahajanwa)
        seed("DMS Test - Rustampur Trauma Centre", "Rustampur Bypass, Gorakhpur", 26.7300, 83.3600,
                FacilityType.TRAUMA_CENTER, 70, 18, 12, 3, true,
                Set.of(MedicalSpeciality.LEVEL_1_TRAUMA, MedicalSpeciality.BURN_UNIT));

        // 5. Gorakhnath Community Hospital (North Gorakhpur)
        seed("DMS Test - Gorakhnath Community Hospital", "Gorakhnath Road, Gorakhpur", 26.7900, 83.3900,
                FacilityType.GENERAL_HOSPITAL, 60, 12, 8, 2, true,
                Set.of(MedicalSpeciality.NEONATAL_ICU, MedicalSpeciality.ISOLATION_WARD));

        log.info("5 DMS test hospitals around Sahjanwa & Gorakhpur ready and synchronized.");
    }

    private void seed(String name, String address, double lat, double lon, FacilityType type,
                      int totalGeneral, int availableGeneral, int totalIcu, int availableIcu,
                      boolean accepting, Set<MedicalSpeciality> specialities) {
        HospitalAdminBinding binding = ADMIN_BINDINGS.get(name);
        String hospitalEmail = (binding != null) ? binding.email() : "dms-test+" + name.toLowerCase().replaceAll("[^a-z0-9]+", "") + "@example.org";
        UUID adminId = (binding != null) ? binding.adminUserId() : null;

        Hospital hospital = hospitals.findByName(name).orElseGet(() -> Hospital.builder()
                .name(name)
                .email(hospitalEmail)
                .phoneNumber("+91987654323" + (ADMIN_BINDINGS.containsKey(name) ? name.length() % 10 : "0"))
                .address(address)
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
                .status(HospitalStatus.ACTIVE)
                .adminUserId(adminId)
                .build());

        // Ensure active administrator linkage and updated metadata
        if (binding != null) {
            hospital.setAdminUserId(binding.adminUserId());
            hospital.setEmail(binding.email());
        }
        hospital.setStatus(HospitalStatus.ACTIVE);
        hospital.setIsAcceptingPatients(accepting);
        hospital = hospitals.save(hospital);

        // Repair spatial index on boot
        try {
            if (geo != null) {
                geo.registerHospitalLocation(hospital.getId(), hospital.getLatitude(), hospital.getLongitude());
            }
        } catch (Exception e) {
            log.debug("Redis spatial registration skipped for hospital {}: {}", name, e.getMessage());
        }
    }
}
