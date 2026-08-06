package com.dms.incident.repository;

import com.dms.incident.entity.Incident;
import com.dms.incident.entity.IncidentStatus;
import com.dms.incident.entity.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    // Find all incidents reported by a specific user with pagination
    Page<Incident> findByReportedBy(UUID reportedBy, Pageable pageable);

    // Filter incidents by status
    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

    // Filter incidents by severity
    Page<Incident> findBySeverity(Severity severity, Pageable pageable);

    // Filter by both status and severity
    Page<Incident> findByStatusAndSeverity(IncidentStatus status, Severity severity, Pageable pageable);

    // Fetch active incidents using Spring Data derived query (Type-safe, no hardcoded strings)
    List<Incident> findByStatusIn(List<IncidentStatus> statuses);

    // Standard B-tree bounding box query for location filtering
    @Query("""
        SELECT i FROM Incident i 
        WHERE i.latitude BETWEEN :minLat AND :maxLat 
          AND i.longitude BETWEEN :minLng AND :maxLng
    """)
    List<Incident> findIncidentsInBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng
    );

    /**
     * Haversine query: Finds active incidents within a circular radius (in Kilometers)
     * created after a specific threshold time.
     * 6371 is Earth's average radius in kilometers.
     */
    @Query("""
        SELECT i FROM Incident i 
        WHERE i.status IN ('REPORTED', 'VERIFIED', 'DISPATCHED') 
          AND i.createdAt >= :timeThreshold 
          AND (6371 * acos(
                cos(radians(:lat)) * cos(radians(i.latitude)) * 
                cos(radians(i.longitude) - radians(:lng)) + 
                sin(radians(:lat)) * sin(radians(i.latitude))
              )) <= :radiusInKm
        ORDER BY i.createdAt ASC
    """)
    List<Incident> findNearbyActiveIncidents(
            @Param("lat") Double latitude,
            @Param("lng") Double longitude,
            @Param("radiusInKm") Double radiusInKm,
            @Param("timeThreshold") LocalDateTime timeThreshold
    );
}