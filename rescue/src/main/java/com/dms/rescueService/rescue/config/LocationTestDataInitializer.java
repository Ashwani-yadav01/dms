package com.dms.rescueService.rescue.config;

import com.dms.rescueService.rescue.entity.DepartmentType;
import com.dms.rescueService.rescue.entity.RescueDepartment;
import com.dms.rescueService.rescue.repository.RescueDepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Idempotent local-development fixtures in the existing rescue department table. */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class LocationTestDataInitializer implements CommandLineRunner {
    private final RescueDepartmentRepository departments;

    @Override
    public void run(String... args) {
        seed("DMS Test - Golghar Fire & Rescue", DepartmentType.FIRE_STATION, 26.7580, 83.3730, true);
        seed("DMS Test - BRD Medical Response", DepartmentType.MEDICAL_UNIT, 26.8133, 83.4025, true);
        seed("DMS Test - Gorakhnath Rescue Post", DepartmentType.DISASTER_RESCUE, 26.7900, 83.3900, true);
        seed("DMS Test - Rustampur Fire Station", DepartmentType.FIRE_STATION, 26.7300, 83.3600, true);
        seed("DMS Test - Chargawan NDRF Unit", DepartmentType.NDRF_BASE, 26.8300, 83.4300, true);
        seed("DMS Test - Pipiganj Rescue Post", DepartmentType.DISASTER_RESCUE, 26.9100, 83.3300, true);
        seed("DMS Test - Sahjanwa Fire Station", DepartmentType.FIRE_STATION, 26.7500, 83.2100, true);
        seed("DMS Test - Bansgaon Response Unit", DepartmentType.MEDICAL_UNIT, 26.5500, 83.3500, true);
        seed("DMS Test - Salempur Response Unit", DepartmentType.DISASTER_RESCUE, 26.3000, 83.9200, true);
        seed("DMS Test - Taramandal Unavailable Unit", DepartmentType.HAZMAT_TEAM, 26.7350, 83.4000, false);
        log.info("Location fixtures ready: {} DMS test rescue departments", 10);
    }

    private void seed(String name, DepartmentType type, double lat, double lon, boolean available) {
        departments.findByName(name).orElseGet(() -> departments.save(RescueDepartment.builder()
                .name(name)
                .type(type)
                .jurisdictionCode("GORAKHPUR")
                .latitude(lat)
                .longitude(lon)
                .contactPhone("0551-000-0000")
                .totalCapacity(5)
                .activeMissionsCount(0)
                .isAvailable(available)
                .build()));
    }
}
