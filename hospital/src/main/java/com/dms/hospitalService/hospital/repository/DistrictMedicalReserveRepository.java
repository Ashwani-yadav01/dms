package com.dms.hospitalService.hospital.repository;

import com.dms.hospitalService.hospital.entity.DistrictMedicalReserve;
import com.dms.hospitalService.hospital.entity.InventoryItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DistrictMedicalReserveRepository extends JpaRepository<DistrictMedicalReserve, UUID> {
    Optional<DistrictMedicalReserve> findByItemType(InventoryItemType itemType);
}
