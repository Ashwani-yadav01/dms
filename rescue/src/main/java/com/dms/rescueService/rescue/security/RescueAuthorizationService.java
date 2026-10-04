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
        if (!"DISTRICT_ADMIN".equals(role())) deny();
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
    public void requireMissionView(RescueMission mission) {
        if ("DISTRICT_ADMIN".equals(role())) return;
        requireAssignedTeam(mission);
    }
    public void requireDepartmentView(UUID departmentId) {
        if ("DISTRICT_ADMIN".equals(role())) return;
        if (!"RESCUE_TEAM".equals(role()) || !claimTrue("rescueVerified")
                || !departmentId.toString().equals(claim("rescueDepartmentId"))) deny();
    }
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
