package com.dms.hospitalService.hospital.repository;

import com.dms.hospitalService.hospital.entity.Hospital;
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
public interface HospitalRepository extends JpaRepository<Hospital, UUID> {

    // 🚨 The critical lock for allocating beds during mass casualties
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Hospital h WHERE h.id = :id")
    Optional<Hospital> findByIdForUpdate(@Param("id") UUID id);

    // Used for the FLOOD algorithm to filter out submerged hospitals
    List<Hospital> findByElevationMetersGreaterThanEqualAndIsAcceptingPatientsTrue(Double elevation);

    // Fetch hospitals by a list of IDs (used after we get nearest IDs from Redis GEO)
    List<Hospital> findByIdInAndIsAcceptingPatientsTrue(List<UUID> ids);
}