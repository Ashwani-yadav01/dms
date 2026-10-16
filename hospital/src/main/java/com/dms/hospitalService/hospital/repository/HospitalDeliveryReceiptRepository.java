package com.dms.hospitalService.hospital.repository;

import com.dms.hospitalService.hospital.entity.HospitalDeliveryReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HospitalDeliveryReceiptRepository extends JpaRepository<HospitalDeliveryReceipt, UUID> {
    boolean existsByDispatchOrderId(UUID dispatchOrderId);
    Optional<HospitalDeliveryReceipt> findByDispatchOrderId(UUID dispatchOrderId);
    List<HospitalDeliveryReceipt> findByHospitalIdOrderByReceivedAtDesc(UUID hospitalId);
}
