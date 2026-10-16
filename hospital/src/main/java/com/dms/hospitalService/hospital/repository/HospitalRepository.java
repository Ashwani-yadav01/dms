package com.dms.hospitalService.hospital.repository;

import com.dms.hospitalService.hospital.entity.Hospital;
import com.dms.hospitalService.hospital.entity.HospitalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Optional<Hospital> findByName(String name);
    Page<Hospital> findByNameContainingIgnoreCaseAndStatus(String name, HospitalStatus status, Pageable pageable);

    // 🚨 The critical lock for allocating beds during mass casualties
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Hospital h WHERE h.id = :id")
    Optional<Hospital> findByIdForUpdate(@Param("id") UUID id);

    // Used for the FLOOD algorithm to filter out submerged hospitals
    List<Hospital> findByElevationMetersGreaterThanEqualAndIsAcceptingPatientsTrue(Double elevation);

    // Fetch hospitals by a list of IDs (used after we get nearest IDs from Redis GEO)
    List<Hospital> findByIdInAndIsAcceptingPatientsTrue(List<UUID> ids);

    // Admin queries
    Page<Hospital> findAll(Pageable pageable);
    Page<Hospital> findByStatus(HospitalStatus status, Pageable pageable);
    List<Hospital> findByStatusAndIsAcceptingPatientsTrue(HospitalStatus status);
    Optional<Hospital> findByEmail(String email);
    Optional<Hospital> findByAdminUserId(UUID adminUserId);
    boolean existsByAdminUserId(UUID adminUserId);
    Page<Hospital> findByAdminUserId(UUID adminUserId, Pageable pageable);
}
