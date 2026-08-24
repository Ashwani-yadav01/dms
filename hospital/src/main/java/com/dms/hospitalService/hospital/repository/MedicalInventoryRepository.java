package com.dms.hospitalService.hospital.repository;

import com.dms.hospitalService.hospital.entity.InventoryItemType;
import com.dms.hospitalService.hospital.entity.MedicalInventory;
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
public interface MedicalInventoryRepository extends JpaRepository<MedicalInventory, UUID> {

    // Fetch all inventory for a specific hospital
    List<MedicalInventory> findByHospitalId(UUID hospitalId);

    // 🚨 Lock a specific item when decreasing stock (e.g. using O-Negative blood)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MedicalInventory m WHERE m.hospital.id = :hospitalId AND m.itemType = :itemType")
    Optional<MedicalInventory> findByHospitalIdAndItemTypeForUpdate(@Param("hospitalId") UUID hospitalId, @Param("itemType") InventoryItemType itemType);

    // AI/Supply Chain Trigger: Find all inventory items that have dropped below their critical threshold
    @Query("SELECT m FROM MedicalInventory m WHERE m.currentQuantity <= m.criticalThreshold")
    List<MedicalInventory> findAllCriticalShortages();
}