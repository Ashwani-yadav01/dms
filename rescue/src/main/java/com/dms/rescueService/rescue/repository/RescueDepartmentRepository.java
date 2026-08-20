package com.dms.rescueService.rescue.repository;

import com.dms.rescueService.rescue.entity.RescueDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RescueDepartmentRepository extends JpaRepository<RescueDepartment, UUID> {

    List<RescueDepartment> findByIsAvailableTrue();

    List<RescueDepartment> findByJurisdictionCode(String jurisdictionCode);

    // PostgreSQL-compatible Haversine spatial query
    @Query(value = """
        SELECT * FROM (
            SELECT d.*, (
                6371 * acos(
                    cos(radians(:lat)) * cos(radians(d.latitude)) *
                    cos(radians(d.longitude) - radians(:lng)) +
                    sin(radians(:lat)) * sin(radians(d.latitude))
                )
            ) AS distance
            FROM tbl_rescue_departments d
            WHERE d.is_available = true 
              AND d.active_missions_count < d.total_capacity
        ) AS nearby_depts
        WHERE nearby_depts.distance <= :radiusKm
        ORDER BY nearby_depts.distance ASC
    """, nativeQuery = true)
    List<RescueDepartment> findAvailableWithinRadius(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radiusKm") Double radiusKm
    );
}