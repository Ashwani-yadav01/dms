package com.dms.logisticsService.logistics.repository;

import com.dms.logisticsService.logistics.entity.InventoryBatch;
import com.dms.logisticsService.logistics.entity.enums.ItemType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, UUID> {

    /**
     * FEFO query with Pessimistic Write Lock:
     * Selects non-expired batches ordered by expiryDate ASC to consume the earliest expiring items first.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b FROM InventoryBatch b
        WHERE b.warehouse.id = :warehouseId
          AND b.itemType = :itemType
          AND (b.quantity - b.reservedQuantity) > 0
          AND b.expiryDate >= :minValidDate
        ORDER BY b.expiryDate ASC
    """)
    List<InventoryBatch> findAvailableBatchesForAllocationLocked(
            @Param("warehouseId") UUID warehouseId,
            @Param("itemType") ItemType itemType,
            @Param("minValidDate") LocalDate minValidDate
    );

    @Query("""
        SELECT COALESCE(SUM(b.quantity - b.reservedQuantity), 0)
        FROM InventoryBatch b
        WHERE b.warehouse.id = :warehouseId
          AND b.itemType = :itemType
          AND b.expiryDate >= :minValidDate
    """)
    Integer getAvailableStockCount(
            @Param("warehouseId") UUID warehouseId,
            @Param("itemType") ItemType itemType,
            @Param("minValidDate") LocalDate minValidDate
    );
}
