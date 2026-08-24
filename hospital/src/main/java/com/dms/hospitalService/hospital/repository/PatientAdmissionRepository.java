package com.dms.hospitalService.hospital.repository;

import com.dms.hospitalService.hospital.entity.PatientAdmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientAdmissionRepository extends JpaRepository<PatientAdmission, UUID> {

    // Find all patients admitted from a specific disaster
    List<PatientAdmission> findByIncidentId(UUID incidentId);

    // Find all currently active patients in a hospital (not yet discharged)
    List<PatientAdmission> findByHospitalIdAndDischargedAtIsNull(UUID hospitalId);
}