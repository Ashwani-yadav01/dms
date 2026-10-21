package com.dms.hospitalService.hospital.repository;

import com.dms.hospitalService.hospital.entity.HospitalSupplyRequest;
import com.dms.hospitalService.hospital.entity.SupplyRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HospitalSupplyRequestRepository extends JpaRepository<HospitalSupplyRequest, UUID> {

    List<HospitalSupplyRequest> findByHospitalIdOrderByCreatedAtDesc(UUID hospitalId);

    @Query("SELECT r FROM HospitalSupplyRequest r ORDER BY " +
           "CASE r.urgencyLevel WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 ELSE 3 END, " +
           "r.createdAt ASC")
    List<HospitalSupplyRequest> findAllPrioritized();

    @Query("SELECT r FROM HospitalSupplyRequest r WHERE r.status = :status ORDER BY " +
           "CASE r.urgencyLevel WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 ELSE 3 END, " +
           "r.createdAt ASC")
    List<HospitalSupplyRequest> findByStatusPrioritized(@Param("status") SupplyRequestStatus status);
}
