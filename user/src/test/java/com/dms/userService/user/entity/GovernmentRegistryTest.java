package com.dms.userService.user.entity;

import com.dms.userService.user.dto.request.GovernmentOfficialProfileRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GovernmentRegistryTest {
    @Test void registryMatchesAllGovernmentVerificationFieldsCaseInsensitively() {
        GovernmentRegistry registry = new GovernmentRegistry();
        registry.setEmployeeId("UP-GOV-18492"); registry.setDepartmentName("DISASTER MANAGEMENT");
        registry.setDesignation("District Officer"); registry.setHierarchyLevel(HierarchyLevel.DISTRICT_OFFICER);
        registry.setAuthorizationCode("GOV-A8F4K2"); registry.setStatus(GovernmentRegistryStatus.ACTIVE);
        GovernmentOfficialProfileRequest request = new GovernmentOfficialProfileRequest();
        request.setEmployeeId("up-gov-18492"); request.setDepartmentName("disaster management");
        request.setDesignation("district officer"); request.setHierarchyLevel(HierarchyLevel.DISTRICT_OFFICER);
        request.setAuthorizationCode("gov-a8f4k2");
        assertTrue(registry.matches(request));
        request.setAuthorizationCode("wrong");
        assertFalse(registry.matches(request));
    }

    @Test void inactiveRegistryEntryIsNotActive() {
        GovernmentRegistry registry = new GovernmentRegistry();
        registry.setStatus(GovernmentRegistryStatus.INACTIVE);
        assertNotEquals(GovernmentRegistryStatus.ACTIVE, registry.getStatus());
    }

    @Test void eachVerificationFieldMustMatch() {
        GovernmentRegistry registry = new GovernmentRegistry();
        registry.setEmployeeId("UP-GOV-18492"); registry.setDepartmentName("DISASTER MANAGEMENT");
        registry.setDesignation("District Officer"); registry.setHierarchyLevel(HierarchyLevel.DISTRICT_OFFICER);
        registry.setAuthorizationCode("GOV-A8F4K2");
        GovernmentOfficialProfileRequest request = new GovernmentOfficialProfileRequest();
        request.setEmployeeId("UP-GOV-18492"); request.setDepartmentName("DISASTER MANAGEMENT");
        request.setDesignation("District Officer"); request.setHierarchyLevel(HierarchyLevel.DISTRICT_OFFICER);
        request.setAuthorizationCode("GOV-A8F4K2");
        request.setEmployeeId("UP-GOV-OTHER"); assertFalse(registry.matches(request));
        request.setEmployeeId("UP-GOV-18492"); request.setDepartmentName("OTHER DEPARTMENT"); assertFalse(registry.matches(request));
        request.setDepartmentName("DISASTER MANAGEMENT"); request.setDesignation("Other"); assertFalse(registry.matches(request));
        request.setDesignation("District Officer"); request.setHierarchyLevel(HierarchyLevel.LOCAL_OFFICER); assertFalse(registry.matches(request));
        request.setHierarchyLevel(HierarchyLevel.DISTRICT_OFFICER); request.setAuthorizationCode("GOV-WRONG"); assertFalse(registry.matches(request));
    }
}
