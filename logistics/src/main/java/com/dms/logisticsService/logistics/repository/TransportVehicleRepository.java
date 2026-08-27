package com.dms.logisticsService.logistics.repository;

import com.dms.logisticsService.logistics.entity.TransportVehicle;
import com.dms.logisticsService.logistics.entity.enums.VehicleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransportVehicleRepository extends JpaRepository<TransportVehicle, UUID> {

    /**
     * Locks the first available vehicle at a given warehouse with sufficient payload capacity.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT v FROM TransportVehicle v
        WHERE v.baseWarehouse.id = :warehouseId
          AND v.status = :status
          AND v.maxPayloadKg >= :minPayloadKg
        ORDER BY v.maxPayloadKg ASC
    """)
    List<TransportVehicle> findAvailableVehiclesLocked(
            @Param("warehouseId") UUID warehouseId,
            @Param("status") VehicleStatus status,
            @Param("minPayloadKg") Double minPayloadKg
    );
}