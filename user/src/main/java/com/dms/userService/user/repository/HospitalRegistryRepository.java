package com.dms.userService.user.repository;

import com.dms.userService.user.entity.HospitalRegistry;
import com.dms.userService.user.entity.HospitalRegistryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HospitalRegistryRepository extends JpaRepository<HospitalRegistry, UUID> {
    Optional<HospitalRegistry> findByEmployeeId(String employeeId);
    Optional<HospitalRegistry> findByAuthorizationCode(String authorizationCode);
    Optional<HospitalRegistry> findByHospitalEmailIgnoreCase(String hospitalEmail);
    Optional<HospitalRegistry> findByHospitalPhone(String hospitalPhone);
    boolean existsByEmployeeId(String employeeId);
    boolean existsByAuthorizationCode(String authorizationCode);
    boolean existsByHospitalEmailIgnoreCase(String hospitalEmail);
    boolean existsByHospitalPhone(String hospitalPhone);
    List<HospitalRegistry> findByStatusOrderByCreatedAtDesc(HospitalRegistryStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE HospitalRegistry h SET h.status = :status WHERE h.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") HospitalRegistryStatus status);
}