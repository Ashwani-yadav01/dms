package com.dms.rescueService.rescue.config;

import com.dms.rescueService.rescue.entity.DepartmentType;
import com.dms.rescueService.rescue.entity.RescueDepartment;
import com.dms.rescueService.rescue.repository.RescueDepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Idempotent local-development fixtures in the existing rescue department table. */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class LocationTestDataInitializer implements CommandLineRunner {
    private final RescueDepartmentRepository departments;

    @Override
    public void run(String... args) {
        seed("DMS Test - Golghar Fire & Rescue", DepartmentType.FIRE_STATION, 26.7580, 83.3730, true,
                UUID.fromString("50000000-0000-0000-0000-000000000012"),
                UUID.fromString("20000000-0000-0000-0000-000000000004"),
                UUID.fromString("30000000-0000-0000-0000-000000000001"));
        seed("DMS Test - BRD Medical Response", DepartmentType.MEDICAL_UNIT, 26.8133, 83.4025, true,
                null, UUID.fromString("20000000-0000-0000-0000-000000000002"), null);
        seed("DMS Test - Gorakhnath Rescue Post", DepartmentType.DISASTER_RESCUE, 26.7900, 83.3900, true,
                null, UUID.fromString("20000000-0000-0000-0000-000000000006"), null);
        seed("DMS Test - Rustampur Fire Station", DepartmentType.FIRE_STATION, 26.7300, 83.3600, true,
                null, UUID.fromString("20000000-0000-0000-0000-000000000004"), null);
        seed("DMS Test - Chargawan NDRF Unit", DepartmentType.NDRF_BASE, 26.8300, 83.4300, true,
                null, UUID.fromString("20000000-0000-0000-0000-000000000006"), null);
        seed("DMS Test - Pipiganj Rescue Post", DepartmentType.DISASTER_RESCUE, 26.9100, 83.3300, true,
                null, UUID.fromString("20000000-0000-0000-0000-000000000006"), null);
        seed("DMS Test - Sahjanwa Fire Station", DepartmentType.FIRE_STATION, 26.7500, 83.2100, true,
                UUID.fromString("50000000-0000-0000-0000-000000000011"),
                UUID.fromString("20000000-0000-0000-0000-000000000004"),
                UUID.fromString("30000000-0000-0000-0000-000000000001"));
        seed("DMS Test - Bansgaon Response Unit", DepartmentType.MEDICAL_UNIT, 26.5500, 83.3500, true,
                null, null, null);
        seed("DMS Test - Salempur Response Unit", DepartmentType.DISASTER_RESCUE, 26.3000, 83.9200, true,
                null, null, null);
        seed("DMS Test - Taramandal Unavailable Unit", DepartmentType.HAZMAT_TEAM, 26.7350, 83.4000, false,
                null, null, null);
        log.info("Location fixtures ready: {} DMS test rescue departments", 10);
    }

    private void seed(String name, DepartmentType type, double lat, double lon, boolean available,
                      UUID preferredId, UUID districtAdminId, UUID governingOfficialId) {
        RescueDepartment dept = departments.findByName(name).orElseGet(() -> {
            RescueDepartment.RescueDepartmentBuilder builder = RescueDepartment.builder()
                    .name(name)
                    .type(type)
                    .jurisdictionCode("GORAKHPUR")
                    .latitude(lat)
                    .longitude(lon)
                    .contactPhone("0551-000-0000")
                    .totalCapacity(5)
                    .activeMissionsCount(0)
                    .isAvailable(available);
            if (preferredId != null) {
                builder.id(preferredId);
            }
            return departments.save(builder.build());
        });

        boolean modified = false;
        if (districtAdminId != null && !districtAdminId.equals(dept.getDistrictAdminId())) {
            dept.setDistrictAdminId(districtAdminId);
            modified = true;
        }
        if (governingOfficialId != null && !governingOfficialId.equals(dept.getGoverningOfficialId())) {
            dept.setGoverningOfficialId(governingOfficialId);
            modified = true;
        }
        if (modified) {
            departments.save(dept);
        }
    }
}
