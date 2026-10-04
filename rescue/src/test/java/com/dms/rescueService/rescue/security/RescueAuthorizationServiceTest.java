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

    private RescueMission mission(UUID departmentId) {
        RescueDepartment department = new RescueDepartment(); department.setId(departmentId);
        RescueMission mission = new RescueMission(); mission.setDepartment(department); return mission;
    }
    private void authenticate(String role, Map<String, Object> claims) {
        var auth = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        auth.setDetails(claims); SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
