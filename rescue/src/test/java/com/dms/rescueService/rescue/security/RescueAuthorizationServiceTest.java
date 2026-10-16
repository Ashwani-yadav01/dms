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
        RescueDepartment owned = new RescueDepartment();
        owned.setId(departmentId);
        owned.setDistrictAdminId(adminId);

        RescueDepartment other = new RescueDepartment();
        other.setId(UUID.randomUUID());
        other.setDistrictAdminId(otherAdminId);

        authenticateWithId(adminId, "DISTRICT_ADMIN", Map.of());
        assertDoesNotThrow(() -> authorization.requireDepartmentAdmin(owned));
        assertThrows(RescueAuthorizationException.class, () -> authorization.requireDepartmentAdmin(other));

        authenticate("SUPER_ADMIN", Map.of());
        assertDoesNotThrow(() -> authorization.requireDepartmentAdmin(other));
    }

    @Test void linkedGovernmentOfficialCanViewDepartmentAndMissions() {
        UUID officialId = UUID.randomUUID();
        UUID otherOfficialId = UUID.randomUUID();

        RescueDepartment dept = new RescueDepartment();
        dept.setId(departmentId);
        dept.setGoverningOfficialId(officialId);

        RescueMission mission = mission(departmentId);
        mission.getDepartment().setGoverningOfficialId(officialId);

        authenticateWithId(officialId, "GOVERNMENT_OFFICIAL", Map.of());
        assertDoesNotThrow(() -> authorization.requireDepartmentView(dept));
        assertDoesNotThrow(() -> authorization.requireMissionView(mission));

        authenticateWithId(otherOfficialId, "GOVERNMENT_OFFICIAL", Map.of());
        assertThrows(RescueAuthorizationException.class, () -> authorization.requireDepartmentView(dept));
        assertThrows(RescueAuthorizationException.class, () -> authorization.requireMissionView(mission));
    }

    private RescueMission mission(UUID departmentId) {
        RescueDepartment department = new RescueDepartment(); department.setId(departmentId);
        RescueMission mission = new RescueMission(); mission.setDepartment(department); return mission;
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
