package com.dms.rescueService.rescue.security;

import com.dms.rescueService.rescue.entity.RescueMission;
import com.dms.rescueService.rescue.exception.RescueAuthorizationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;

@Service
public class RescueAuthorizationService {
    public void requireDispatcher() {
        if (!"DISTRICT_ADMIN".equals(role()) && !"SUPER_ADMIN".equals(role())) deny();
    }
    public UUID currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UUID id ? id : null;
    }
    public void requireAssignedTeam(RescueMission mission) {
        String departmentId = claim("rescueDepartmentId");
        if (!"RESCUE_TEAM".equals(role()) || !claimTrue("rescueVerified") || departmentId == null
                || !mission.getDepartment().getId().toString().equals(departmentId)) deny();
    }
    public void requireAssignedTeamOrDispatcher(RescueMission mission) {
        if ("DISTRICT_ADMIN".equals(role()) || "SUPER_ADMIN".equals(role())) return;
        requireAssignedTeam(mission);
    }
    public void requireDepartmentAdmin(com.dms.rescueService.rescue.entity.RescueDepartment dept) {
        if ("SUPER_ADMIN".equals(role())) return;
        if ("DISTRICT_ADMIN".equals(role())) {
            UUID actorId = currentUserIdOrNull();
            if (actorId != null && dept.getDistrictAdminId() != null && dept.getDistrictAdminId().equals(actorId)) {
                return;
            }
        }
        deny();
    }
    public void requireDepartmentView(com.dms.rescueService.rescue.entity.RescueDepartment dept) {
        if ("SUPER_ADMIN".equals(role())) return;
        UUID actorId = currentUserIdOrNull();
        if ("DISTRICT_ADMIN".equals(role())) {
            if (actorId != null && dept.getDistrictAdminId() != null && dept.getDistrictAdminId().equals(actorId)) {
                return;
            }
        }
        if ("GOVERNMENT_OFFICIAL".equals(role())) {
            if (actorId != null && dept.getGoverningOfficialId() != null && dept.getGoverningOfficialId().equals(actorId)) {
                return;
            }
        }
        if ("RESCUE_TEAM".equals(role()) && claimTrue("rescueVerified")
                && dept.getId().toString().equals(claim("rescueDepartmentId"))) {
            return;
        }
        deny();
    }
    public void requireMissionView(RescueMission mission) {
        if ("SUPER_ADMIN".equals(role())) return;
        UUID actorId = currentUserIdOrNull();
        if ("DISTRICT_ADMIN".equals(role())) {
            if (actorId != null && mission.getDepartment().getDistrictAdminId() != null
                    && mission.getDepartment().getDistrictAdminId().equals(actorId)) {
                return;
            }
        }
        if ("GOVERNMENT_OFFICIAL".equals(role())) {
            if (actorId != null && mission.getDepartment().getGoverningOfficialId() != null
                    && mission.getDepartment().getGoverningOfficialId().equals(actorId)) {
                return;
            }
        }
        requireAssignedTeam(mission);
    }
    public void requireDepartmentView(UUID departmentId) {
        if ("SUPER_ADMIN".equals(role()) || "DISTRICT_ADMIN".equals(role())) return;
        if (!"RESCUE_TEAM".equals(role()) || !claimTrue("rescueVerified")
                || !departmentId.toString().equals(claim("rescueDepartmentId"))) deny();
    }
    public String currentRole() { return role(); }
    public boolean isSuperAdmin() { return "SUPER_ADMIN".equals(role()); }
    public boolean isDistrictAdmin() { return "DISTRICT_ADMIN".equals(role()); }
    public boolean isGovernmentOfficial() { return "GOVERNMENT_OFFICIAL".equals(role()); }
    public boolean isRescueTeam() { return "RESCUE_TEAM".equals(role()); }
    private String role() { return authentication().getAuthorities().stream().findFirst().map(a -> a.getAuthority().replace("ROLE_", "")).orElse(""); }
    private boolean claimTrue(String name) { Object v = details().get(name); return Boolean.TRUE.equals(v); }
    private String claim(String name) { Object v = details().get(name); return v == null ? null : v.toString(); }
    private Map<?, ?> details() { Object value = authentication().getDetails(); return value instanceof Map<?, ?> m ? m : Map.of(); }
    private Authentication authentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) deny();
        return auth;
    }
    private void deny() { throw new RescueAuthorizationException("You are not authorized to perform this action."); }
}
