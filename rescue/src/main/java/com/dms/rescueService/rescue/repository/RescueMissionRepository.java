package com.dms.rescueService.rescue.repository;

import com.dms.rescueService.rescue.entity.MissionStatus;
import com.dms.rescueService.rescue.entity.RescueMission;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RescueMissionRepository extends JpaRepository<RescueMission, UUID> {

    List<RescueMission> findByIncidentId(UUID incidentId);

    List<RescueMission> findByDepartmentId(UUID departmentId);

    List<RescueMission> findByStatus(MissionStatus status);

    /**
     * Pessimistic lock for updating an existing mission's status safely.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM RescueMission m WHERE m.id = :id")
    Optional<RescueMission> findByIdWithLock(@Param("id") UUID id);

    /**
     * Active missions for SLA check fetched with a pessimistic lock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT m FROM RescueMission m 
        WHERE m.status IN ('DISPATCHED', 'EN_ROUTE', 'ON_SCENE') 
        AND m.isSlaBreached = false
    """)
    List<RescueMission> findActiveMissionsForSlaCheckWithLock();
}