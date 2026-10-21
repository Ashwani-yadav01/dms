package com.dms.userService.user.repository;

import com.dms.userService.user.entity.DistrictAdminRegistry;
import com.dms.userService.user.entity.DistrictAdminRegistryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DistrictAdminRegistryRepository extends JpaRepository<DistrictAdminRegistry, UUID> {

    Optional<DistrictAdminRegistry> findByAuthorizationCode(String authorizationCode);

    Optional<DistrictAdminRegistry> findByAdminEmailIgnoreCase(String adminEmail);

    List<DistrictAdminRegistry> findByStatusOrderByCreatedAtDesc(DistrictAdminRegistryStatus status);

    boolean existsByEmployeeId(String employeeId);

    boolean existsByAuthorizationCode(String authorizationCode);

    boolean existsByAdminEmailIgnoreCase(String adminEmail);

    boolean existsByAdminPhone(String adminPhone);
}
