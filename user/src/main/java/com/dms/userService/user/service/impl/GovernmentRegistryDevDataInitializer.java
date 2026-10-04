package com.dms.userService.user.service.impl;

import com.dms.userService.user.entity.GovernmentRegistry;
import com.dms.userService.user.entity.GovernmentRegistryStatus;
import com.dms.userService.user.entity.HierarchyLevel;
import com.dms.userService.user.repository.GovernmentRegistryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GovernmentRegistryDevDataInitializer implements ApplicationRunner {
    private final GovernmentRegistryRepository registryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed("UP-GOV-18492", "DISASTER MANAGEMENT", "District Officer",
                HierarchyLevel.DISTRICT_OFFICER, "GOV-A8F4K2", 26.8467, 80.9462,
                GovernmentRegistryStatus.ACTIVE);
        seed("UP-GOV-29481", "State Emergency Operations", "Emergency Coordinator",
                HierarchyLevel.STATE_OFFICER, "GOV-M7P2Q9", 26.4499, 80.3319,
                GovernmentRegistryStatus.ACTIVE);
        seed("UP-GOV-38127", "Fire and Emergency Services", "Response Officer",
                HierarchyLevel.DISTRICT_OFFICER, "GOV-R4K8L1", 25.3176, 82.9739,
                GovernmentRegistryStatus.ACTIVE);
        seed("UP-GOV-49106", "Disaster Management", "Inactive Test Officer",
                HierarchyLevel.LOCAL_OFFICER, "GOV-X2N6B9", null, null,
                GovernmentRegistryStatus.INACTIVE);
    }

    private void seed(String employeeId, String department, String designation,
                      HierarchyLevel hierarchy, String code, Double latitude,
                      Double longitude, GovernmentRegistryStatus status) {
        if (registryRepository.findByEmployeeId(employeeId).isPresent()) return;

        GovernmentRegistry registry = new GovernmentRegistry();
        registry.setEmployeeId(employeeId);
        registry.setDepartmentName(department);
        registry.setDesignation(designation);
        registry.setHierarchyLevel(hierarchy);
        registry.setAuthorizationCode(code);
        registry.setOfficeLatitude(latitude);
        registry.setOfficeLongitude(longitude);
        registry.setDutyRadiusKm(20.0);
        registry.setStatus(status);
        registryRepository.save(registry);
    }
}
