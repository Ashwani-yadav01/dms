package com.dms.hospitalService.hospital.repository;

import com.dms.hospitalService.hospital.entity.HospitalAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HospitalAuditRepository extends JpaRepository<HospitalAudit, UUID> {
    List<HospitalAudit> findByHospitalIdOrderByOccurredAtDesc(UUID hospitalId);
}
