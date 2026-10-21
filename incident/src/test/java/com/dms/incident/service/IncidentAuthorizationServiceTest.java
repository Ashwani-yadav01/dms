package com.dms.incident.service;

import com.dms.incident.entity.Incident;
import com.dms.incident.entity.IncidentStatus;
import com.dms.incident.entity.IncidentType;
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

    @Test void governmentOfficialCannotVerifyReportedIncidents() {
        Incident nearby = incident(citizen, null, null, IncidentStatus.REPORTED);
        nearby.setIncidentType(IncidentType.FLOOD);
        nearby.setLatitude(26.9); nearby.setLongitude(80.9);
        Map<String, Object> claims = Map.of("govVerified", true, "officialStatus", "AVAILABLE",
                "departmentCategory", "DISASTER_RESCUE",
                "officialLatitude", 26.8467, "officialLongitude", 80.9462, "officialDutyRadiusKm", 20.0);
        authenticate(official, "GOVERNMENT_OFFICIAL", claims);
        assertThrows(IncidentAuthorizationException.class, () -> authorization.requireOfficialAssignment(nearby));

        Incident fire = incident(citizen, null, null, IncidentStatus.REPORTED);
        fire.setIncidentType(IncidentType.FIRE);
        fire.setLatitude(26.9); fire.setLongitude(80.9);
        authenticate(official, "GOVERNMENT_OFFICIAL", Map.of("govVerified", true, "officialStatus", "AVAILABLE",
                "departmentCategory", "FIRE_SAFETY",
                "officialLatitude", 26.8467, "officialLongitude", 80.9462, "officialDutyRadiusKm", 20.0));
        assertThrows(IncidentAuthorizationException.class, () -> authorization.requireOfficialAssignment(fire));
    }

    @Test void governmentOfficialCanResolveDispatchedAndVerifiedIncidents() {
        Incident dispatched = incident(citizen, null, null, IncidentStatus.DISPATCHED);
        Map<String, Object> claims = Map.of("govVerified", true, "officialStatus", "AVAILABLE",
                "departmentCategory", "FIRE_SAFETY",
                "officialLatitude", 26.8467, "officialLongitude", 80.9462, "officialDutyRadiusKm", 20.0);
        authenticate(official, "GOVERNMENT_OFFICIAL", claims);
        assertDoesNotThrow(() -> authorization.requireOfficialAssignment(dispatched));

        Incident verified = incident(citizen, null, null, IncidentStatus.VERIFIED);
        assertDoesNotThrow(() -> authorization.authorize(verified, com.dms.incident.entity.IncidentAction.RESOLVE));
    }

    @Test void unverifiedGovernmentOfficialCannotResolveIncident() {
        Incident dispatched = incident(citizen, null, null, IncidentStatus.DISPATCHED);
        authenticate(official, "GOVERNMENT_OFFICIAL", Map.of("govVerified", false, "officialStatus", "OFFLINE"));
        assertThrows(IncidentAuthorizationException.class, () -> authorization.requireOfficialAssignment(dispatched));
    }

    @Test void adminCanVerifyAnyIncidentRegardlessOfDepartmentOrLocation() {
        Incident fire = incident(citizen, null, null, IncidentStatus.REPORTED);
        fire.setIncidentType(IncidentType.FIRE);
        fire.setLatitude(50.0); fire.setLongitude(50.0);

        authenticate(UUID.randomUUID(), "DISTRICT_ADMIN", Map.of());
        assertDoesNotThrow(() -> authorization.requireOfficialAssignment(fire));

        authenticate(UUID.randomUUID(), "SUPER_ADMIN", Map.of());
        assertDoesNotThrow(() -> authorization.requireOfficialAssignment(fire));
    }

    @Test void incidentAssignmentRequiresDistrictAdministratorRole() {
        authenticate(UUID.randomUUID(), "GOVERNMENT_OFFICIAL", Map.of("govVerified", true, "officialStatus", "AVAILABLE"));
        assertThrows(IncidentAuthorizationException.class, authorization::requireDistrictAdmin);
        authenticate(UUID.randomUUID(), "DISTRICT_ADMIN", Map.of());
        assertDoesNotThrow(authorization::requireDistrictAdmin);
        authenticate(UUID.randomUUID(), "SUPER_ADMIN", Map.of());
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
        Incident incident = new Incident();
        incident.setReportedBy(owner);
        incident.setAssignedDepartmentId(assignedDepartment);
        incident.setAssignedOfficialId(assignedOfficial);
        incident.setStatus(status);
        incident.setIncidentType(IncidentType.FLOOD);
        return incident;
    }

    private void authenticate(UUID id, String role, Map<String, Object> claims) {
        var authentication = new UsernamePasswordAuthenticationToken(id, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        authentication.setDetails(claims);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
