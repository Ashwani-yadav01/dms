package com.dms.rescueService.rescue.security;

import com.dms.rescueService.rescue.entity.RescueDepartment;
import com.dms.rescueService.rescue.entity.RescueMission;
import com.dms.rescueService.rescue.exception.RescueAuthorizationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class RescueAuthorizationServiceTest {
    private final RescueAuthorizationService authorization = new RescueAuthorizationService();
    private final UUID departmentId = UUID.randomUUID();
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void teamCanOnlyOperateOnMissionsForItsVerifiedDepartment() {
        authenticate("RESCUE_TEAM", Map.of("rescueVerified", true, "rescueDepartmentId", departmentId.toString()));
        assertDoesNotThrow(() -> authorization.requireAssignedTeam(mission(departmentId)));
        assertThrows(RescueAuthorizationException.class, () -> authorization.requireAssignedTeam(mission(UUID.randomUUID())));
        authenticate("RESCUE_TEAM", Map.of("rescueVerified", false, "rescueDepartmentId", departmentId.toString()));
        assertThrows(RescueAuthorizationException.class, () -> authorization.requireAssignedTeam(mission(departmentId)));
    }

    @Test void dispatchNeedsProvisionedDistrictAdministrator() {
        authenticate("GOVERNMENT_OFFICIAL", Map.of("govVerified", true));
        assertThrows(RescueAuthorizationException.class, authorization::requireDispatcher);
        authenticate("DISTRICT_ADMIN", Map.of());
        assertDoesNotThrow(authorization::requireDispatcher);
    }

    @Test void districtAdminCanOnlyManageOwnedDepartments() {
        UUID adminId = UUID.randomUUID();
        UUID otherAdminId = UUID.randomUUID();
        RescueDepartment owned = RescueDepartment.builder().id(departmentId).districtAdminId(adminId).build();
        RescueDepartment other = RescueDepartment.builder().id(UUID.randomUUID()).districtAdminId(otherAdminId).build();

        // 1. Scoped district admin (e.g. HEALTH category) can only manage owned department
        authenticateWithId(adminId, "DISTRICT_ADMIN", Map.of("departmentCategory", "HEALTH"));
        assertDoesNotThrow(() -> authorization.requireDepartmentAdmin(owned));
        assertThrows(RescueAuthorizationException.class, () -> authorization.requireDepartmentAdmin(other));

        // 2. Department Management District Admin can manage any department in district
        authenticateWithId(adminId, "DISTRICT_ADMIN", Map.of("departmentCategory", "DEPARTMENT_MANAGEMENT"));
        assertDoesNotThrow(() -> authorization.requireDepartmentAdmin(other));

        authenticate("SUPER_ADMIN", Map.of());
        assertDoesNotThrow(() -> authorization.requireDepartmentAdmin(other));
    }

    @Test void linkedGovernmentOfficialCanViewDepartmentAndMissions() {
        UUID officialId = UUID.randomUUID();
        UUID otherOfficialId = UUID.randomUUID();

        RescueDepartment dept = RescueDepartment.builder()
                .id(departmentId)
                .governingOfficialId(officialId)
                .build();

        RescueMission mission = mission(departmentId);
        mission.getDepartment().setGoverningOfficialId(officialId);

        authenticateWithId(officialId, "GOVERNMENT_OFFICIAL", Map.of());
        assertDoesNotThrow(() -> authorization.requireDepartmentView(dept));
        assertDoesNotThrow(() -> authorization.requireMissionView(mission));

        authenticateWithId(otherOfficialId, "GOVERNMENT_OFFICIAL", Map.of());
        assertThrows(RescueAuthorizationException.class, () -> authorization.requireDepartmentView(dept));
        assertThrows(RescueAuthorizationException.class, () -> authorization.requireMissionView(mission));
    }

    @Test void stationChiefCanViewAndOperateMissions() {
        UUID chiefId = UUID.randomUUID();
        RescueDepartment dept = RescueDepartment.builder()
                .id(departmentId)
                .stationChiefId(chiefId)
                .build();

        RescueMission mission = mission(departmentId);
        mission.getDepartment().setStationChiefId(chiefId);

        authenticateWithId(chiefId, "GOVERNMENT_OFFICIAL", Map.of());
        assertDoesNotThrow(() -> authorization.requireDepartmentView(dept));
        assertDoesNotThrow(() -> authorization.requireMissionView(mission));
        assertDoesNotThrow(() -> authorization.requireAssignedTeam(mission));
    }

    private RescueMission mission(UUID departmentId) {
        RescueDepartment department = RescueDepartment.builder().id(departmentId).build();
        RescueMission mission = RescueMission.builder().department(department).build();
        return mission;
    }
    private void authenticate(String role, Map<String, Object> claims) {
        authenticateWithId(UUID.randomUUID(), role, claims);
    }
    private void authenticateWithId(UUID id, String role, Map<String, Object> claims) {
        var auth = new UsernamePasswordAuthenticationToken(id, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        auth.setDetails(claims); SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
