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

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    // Find all incidents reported by a specific user
    Page<Incident> findByReportedBy(UUID reportedBy, Pageable pageable);

    // Filter incidents by status
    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

    // Filter incidents by severity
    Page<Incident> findBySeverity(Severity severity, Pageable pageable);

    // Filter by both status and severity
    Page<Incident> findByStatusAndSeverity(IncidentStatus status, Severity severity, Pageable pageable);

    // Fetch active incidents (REPORTED, VERIFIED, DISPATCHED)
    @Query("SELECT i FROM Incident i WHERE i.status IN ('REPORTED', 'VERIFIED', 'DISPATCHED')")
    List<Incident> findAllActiveIncidents();

    // Standard B-tree bounding box query for quick location filtering
    @Query("SELECT i FROM Incident i WHERE i.latitude BETWEEN :minLat AND :maxLat AND i.longitude BETWEEN :minLng AND :maxLng")
    List<Incident> findIncidentsInBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng
    );
}