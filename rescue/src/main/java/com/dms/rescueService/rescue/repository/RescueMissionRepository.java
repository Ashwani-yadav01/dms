package com.dms.rescueService.rescue.repository;

import com.dms.rescueService.rescue.entity.MissionStatus;
import com.dms.rescueService.rescue.entity.RescueMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RescueMissionRepository extends JpaRepository<RescueMission, UUID> {

    List<RescueMission> findByIncidentId(UUID incidentId);

    List<RescueMission> findByDepartmentId(UUID departmentId);

    List<RescueMission> findByStatus(MissionStatus status);

    // Finds active missions that exceeded SLA and are not yet marked breached
    @Query("""
        SELECT m FROM RescueMission m 
        WHERE m.status IN ('DISPATCHED', 'EN_ROUTE', 'ON_SCENE') 
        AND m.isSlaBreached = false
    """)
    List<RescueMission> findActiveMissionsForSlaCheck();
}