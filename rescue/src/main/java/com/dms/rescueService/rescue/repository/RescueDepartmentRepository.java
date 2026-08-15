package com.dms.rescueService.rescue.repository;

import com.dms.rescueService.rescue.entity.RescueDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RescueDepartmentRepository extends JpaRepository<RescueDepartment, UUID> {

    List<RescueDepartment> findByJurisdictionCode(String jurisdictionCode);

    // Haversine spatial query to locate available departments within radius
    @Query(value = """
        SELECT d.*, (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(d.latitude)) *
                cos(radians(d.longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(d.latitude))
            )
        ) AS distance
        FROM tbl_rescue_departments d
        WHERE d.is_available = true
        HAVING distance <= :radiusKm
        ORDER BY distance ASC
    """, nativeQuery = true)
    List<RescueDepartment> findAvailableWithinRadius(Double lat, Double lng, Double radiusKm);
}