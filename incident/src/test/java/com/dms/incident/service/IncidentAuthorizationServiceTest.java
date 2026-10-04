package com.dms.incident.service;

import com.dms.incident.entity.Incident;
import com.dms.incident.entity.IncidentStatus;
import com.dms.incident.exception.IncidentAuthorizationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class IncidentAuthorizationServiceTest {
    private final IncidentAuthorizationService authorization = new IncidentAuthorizationService();
    private final UUID citizen = UUID.randomUUID();
    private final UUID official = UUID.randomUUID();
    private final UUID department = UUID.randomUUID();

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void citizenCanViewOnlyTheirOwnIncident() {
        authenticate(citizen, "CITIZEN", Map.of());
        assertDoesNotThrow(() -> authorization.requireView(incident(citizen, null, null, IncidentStatus.REPORTED)));
        assertThrows(IncidentAuthorizationException.class,
                () -> authorization.requireView(incident(UUID.randomUUID(), null, null, IncidentStatus.REPORTED)));
    }

    @Test void citizenCanEditOwnReportedIncidentButNotTerminalIncident() {
        authenticate(citizen, "CITIZEN", Map.of());
        assertDoesNotThrow(() -> authorization.requireOwnerEditable(incident(citizen, null, null, IncidentStatus.REPORTED)));
        assertThrows(IncidentAuthorizationException.class,
                () -> authorization.requireOwnerEditable(incident(citizen, null, null, IncidentStatus.RESOLVED)));
        assertThrows(IncidentAuthorizationException.class,
                () -> authorization.requireOwnerEditable(incident(UUID.randomUUID(), null, null, IncidentStatus.REPORTED)));
    }

    @Test void governmentRoleAloneCannotViewOrManageAnIncident() {
        authenticate(official, "GOVERNMENT_OFFICIAL", Map.of("govVerified", true));
        Incident unassigned = incident(citizen, null, null, IncidentStatus.REPORTED);
        assertThrows(IncidentAuthorizationException.class, () -> authorization.requireView(unassigned));
        assertThrows(IncidentAuthorizationException.class, () -> authorization.requireOfficialAssignment(unassigned));
    }

    @Test void anyVerifiedOfficialInTheirDutyAreaCanVerifyWithoutAssignment() {
        Incident nearby = incident(citizen, null, null, IncidentStatus.REPORTED);
        nearby.setLatitude(26.9); nearby.setLongitude(80.9);
        Map<String, Object> claims = Map.of("govVerified", true, "officialStatus", "AVAILABLE",
                "officialLatitude", 26.8467, "officialLongitude", 80.9462, "officialDutyRadiusKm", 20.0);
        authenticate(official, "GOVERNMENT_OFFICIAL", claims);
        assertDoesNotThrow(() -> authorization.requireOfficialAssignment(nearby));

        Incident outside = incident(citizen, null, null, IncidentStatus.REPORTED);
        outside.setLatitude(27.2); outside.setLongitude(81.5);
        assertThrows(IncidentAuthorizationException.class, () -> authorization.requireOfficialAssignment(outside));

        authenticate(official, "GOVERNMENT_OFFICIAL", Map.of("govVerified", false, "officialStatus", "AVAILABLE",
                "officialLatitude", 26.8467, "officialLongitude", 80.9462, "officialDutyRadiusKm", 20.0));
        assertThrows(IncidentAuthorizationException.class, () -> authorization.requireOfficialAssignment(nearby));
    }

    @Test void explicitAssignmentDoesNotLetOfficialActOutsideDutyArea() {
        authenticate(official, "GOVERNMENT_OFFICIAL", Map.of("govVerified", true,
                "officialLatitude", 0.0, "officialLongitude", 0.0, "officialDutyRadiusKm", 25.0,
                "officialStatus", "AVAILABLE"));
        Incident nearby = incident(citizen, null, null, IncidentStatus.REPORTED);
        nearby.setLatitude(0.1); nearby.setLongitude(0.1);
        assertThrows(IncidentAuthorizationException.class, () -> authorization.requireOfficialAssignment(nearby));
        nearby.setAssignedOfficialId(official);
        assertThrows(IncidentAuthorizationException.class, () -> authorization.requireOfficialAssignment(nearby));
        Incident distant = incident(citizen, null, null, IncidentStatus.REPORTED);
        distant.setLatitude(1.0); distant.setLongitude(1.0);
        assertThrows(IncidentAuthorizationException.class, () -> authorization.requireOfficialAssignment(distant));
    }

    @Test void incidentAssignmentRequiresDistrictAdministratorRole() {
        authenticate(UUID.randomUUID(), "GOVERNMENT_OFFICIAL", Map.of("govVerified", true, "officialStatus", "AVAILABLE"));
        assertThrows(IncidentAuthorizationException.class, authorization::requireDistrictAdmin);
        authenticate(UUID.randomUUID(), "DISTRICT_ADMIN", Map.of());
        assertDoesNotThrow(authorization::requireDistrictAdmin);
    }

    @Test void rescueTeamNeedsVerifiedProfileAndMatchingAssignedDepartment() {
        Incident assigned = incident(citizen, department, null, IncidentStatus.DISPATCHED);
        authenticate(UUID.randomUUID(), "RESCUE_TEAM", Map.of("rescueVerified", true, "rescueDepartmentId", UUID.randomUUID().toString()));
        assertThrows(IncidentAuthorizationException.class, () -> authorization.requireRescueAssignment(assigned));
        authenticate(UUID.randomUUID(), "RESCUE_TEAM", Map.of("rescueVerified", false, "rescueDepartmentId", department.toString()));
        assertThrows(IncidentAuthorizationException.class, () -> authorization.requireRescueAssignment(assigned));
        authenticate(UUID.randomUUID(), "RESCUE_TEAM", Map.of("rescueVerified", true, "rescueDepartmentId", department.toString()));
        assertDoesNotThrow(() -> authorization.requireRescueAssignment(assigned));
    }

    private Incident incident(UUID owner, UUID assignedDepartment, UUID assignedOfficial, IncidentStatus status) {
        Incident incident = new Incident(); incident.setReportedBy(owner); incident.setAssignedDepartmentId(assignedDepartment);
        incident.setAssignedOfficialId(assignedOfficial); incident.setStatus(status); return incident;
    }
    private void authenticate(UUID id, String role, Map<String, Object> claims) {
        var authentication = new UsernamePasswordAuthenticationToken(id, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        authentication.setDetails(claims);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
