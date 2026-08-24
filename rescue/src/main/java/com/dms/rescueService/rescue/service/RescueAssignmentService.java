package com.dms.rescueService.rescue.service;

import com.dms.common.events.IncidentCreatedEvent;
import com.dms.rescueService.rescue.entity.RescueMission;

import java.util.UUID;

public interface RescueAssignmentService {

    /**
     * Consumes an incoming IncidentCreatedEvent and automatically assigns
     * an available rescue department/team based on spatial proximity and capacity.
     *
     * @param event The incident creation payload published by the Incident Service.
     */
    void autoAssignRescueTeam(IncidentCreatedEvent event);

    /**
     * Manually reassigns or assigns a mission to a specific department
     * in cases where automated routing fails or manual override is required.
     *
     * @param incidentId   The target incident ID.
     * @param departmentId The target rescue department ID.
     * @return The created or updated RescueMission entity.
     */
    RescueMission assignDepartmentToIncident(UUID incidentId, UUID departmentId);

    /**
     * Manually assigns a mission to a specific department with incident coordinates
     * for spatial distance calculations and Redis GEO tracking.
     *
     * @param incidentId   The target incident ID.
     * @param departmentId The target rescue department ID.
     * @param incidentLat  The incident latitude.
     * @param incidentLon  The incident longitude.
     * @return The created or updated RescueMission entity.
     */
    RescueMission assignDepartmentToIncident(UUID incidentId, UUID departmentId, double incidentLat, double incidentLon);

 RescueMission completeRescueMission(UUID missionId, int victimsRescued);
}