package com.dms.incident.service;

import com.dms.incident.entity.Incident;
import com.dms.incident.entity.IncidentAction;
import com.dms.incident.exception.IncidentAuthorizationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;

@Service
public class IncidentAuthorizationService {
    public UUID userId() {
        Object principal = authentication().getPrincipal();
        if (principal instanceof UUID id) return id;
        throw new IncidentAuthorizationException("You are not authorized to perform this action.");
    }
    public String role() {
        return authentication().getAuthorities().stream().map(a -> a.getAuthority()).findFirst()
                .orElse("").replace("ROLE_", "");
    }
    public boolean claimTrue(String name) {
        Object details = authentication().getDetails();
        return details instanceof Map<?, ?> claims && Boolean.TRUE.equals(claims.get(name));
    }
    public String claim(String name) {
        Object details = authentication().getDetails();
        Object value = details instanceof Map<?, ?> claims ? claims.get(name) : null;
        return value == null ? null : value.toString();
    }
    public void requireCitizenCreate() {
        authorize(null, IncidentAction.CREATE);
    }
    public void requireDistrictAdmin() {
        if (!"DISTRICT_ADMIN".equals(role())) deny();
    }
    public void requireOwnerEditable(Incident incident) {
        authorize(incident, IncidentAction.UPDATE);
    }
    public void requireView(Incident incident) {
        authorize(incident, IncidentAction.VIEW);
    }
    public boolean canView(Incident incident) {
        UUID actor = userId();
        return ("CITIZEN".equals(role()) && actor.equals(incident.getReportedBy()))
                || isVerifiedOfficialWithinRange(incident) || isAssignedRescueTeam(incident);
    }
    public void requireVerifiedOfficial() { if (!isVerifiedOfficialIdentity()) deny(); }
    public void requireOfficialAssignment(Incident incident) {
        IncidentAction action = incident.getStatus() == com.dms.incident.entity.IncidentStatus.DISPATCHED
                ? IncidentAction.RESOLVE : IncidentAction.VERIFY;
        authorize(incident, action);
    }
    public void authorize(Incident incident, IncidentAction action) {
        UUID actor = userId();
        if (action == IncidentAction.CREATE || action == IncidentAction.ASSIGN_OFFICIAL) {
            if (action == IncidentAction.CREATE && !"CITIZEN".equals(role())) deny();
            if (action == IncidentAction.ASSIGN_OFFICIAL && !"DISTRICT_ADMIN".equals(role())) deny();
            return;
        }
        if (incident == null) deny();
        boolean ownerEditable = "CITIZEN".equals(role()) && actor.equals(incident.getReportedBy())
                && incident.getStatus() == com.dms.incident.entity.IncidentStatus.REPORTED;
        boolean officialInRange = isVerifiedOfficialWithinRange(incident);
        boolean assignedOfficial = isVerifiedAssignedOfficial(incident, actor);
        boolean allowed = switch (action) {
            case VIEW -> canView(incident);
            case UPDATE, DELETE -> ownerEditable;
            case VERIFY, REJECT -> officialInRange && incident.getStatus() == com.dms.incident.entity.IncidentStatus.REPORTED;
            case ASSIGN_OFFICIAL -> false;
            case RESOLVE -> assignedOfficial && incident.getStatus() == com.dms.incident.entity.IncidentStatus.DISPATCHED;
            case CREATE -> false;
        };
        if (!allowed) deny();
    }
    public void requireRescueAssignment(Incident incident) {
        if (!isAssignedRescueTeam(incident)) deny();
    }
    private boolean isVerifiedAssignedOfficial(Incident incident, UUID actor) {
        if (!isVerifiedOfficialIdentity()) return false;
        return actor.equals(incident.getAssignedOfficialId());
    }
    private boolean isVerifiedOfficialIdentity() {
        if (!"GOVERNMENT_OFFICIAL".equals(role()) || !claimTrue("govVerified")) return false;
        String status = claim("officialStatus");
        return "AVAILABLE".equals(status) || "ON_DUTY".equals(status);
    }
    private boolean isVerifiedOfficialWithinRange(Incident incident) {
        DutyArea area = officialDutyArea();
        if (area == null || incident.getLatitude() == null || incident.getLongitude() == null) return false;
        return distanceKm(area.latitude(), area.longitude(), incident.getLatitude(), incident.getLongitude())
                <= area.radiusKm();
    }
    private DutyArea officialDutyArea() {
        if (!isVerifiedOfficialIdentity()) return null;
        try {
            double latitude = Double.parseDouble(claim("officialLatitude"));
            double longitude = Double.parseDouble(claim("officialLongitude"));
            double radiusKm = Double.parseDouble(claim("officialDutyRadiusKm"));
            if (!Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0
                    || !Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0
                    || !Double.isFinite(radiusKm) || radiusKm <= 0.0 || radiusKm > 500.0) return null;
            return new DutyArea(latitude, longitude, radiusKm);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }
    private boolean isAssignedRescueTeam(Incident incident) {
        String department = claim("rescueDepartmentId");
        return "RESCUE_TEAM".equals(role()) && claimTrue("rescueVerified") && department != null
                && incident.getAssignedDepartmentId() != null
                && incident.getAssignedDepartmentId().toString().equals(department);
    }
    private Authentication authentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) deny();
        return auth;
    }
    private void deny() { throw new IncidentAuthorizationException("You are not authorized to perform this action."); }

    public record DutyArea(double latitude, double longitude, double radiusKm) {}

}
