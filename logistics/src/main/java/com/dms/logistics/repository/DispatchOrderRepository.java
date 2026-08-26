package com.dms.logistics.repository;

import com.dms.logistics.entity.DispatchOrder;
import com.dms.logistics.entity.enums.DispatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DispatchOrderRepository extends JpaRepository<DispatchOrder, UUID> {
    List<DispatchOrder> findByTargetHospitalId(UUID hospitalId);
    List<DispatchOrder> findByStatus(DispatchStatus status);
}