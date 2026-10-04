package com.dms.userService.user.repository;

import com.dms.userService.user.entity.GovernmentRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface GovernmentRegistryRepository extends JpaRepository<GovernmentRegistry, UUID> {
    Optional<GovernmentRegistry> findByEmployeeId(String employeeId);
    Optional<GovernmentRegistry> findByAuthorizationCode(String authorizationCode);
    Optional<GovernmentRegistry> findByOfficialEmailIgnoreCase(String officialEmail);
    boolean existsByEmployeeId(String employeeId);
    boolean existsByAuthorizationCode(String authorizationCode);
    boolean existsByOfficialEmailIgnoreCase(String officialEmail);
    boolean existsByOfficialPhone(String officialPhone);
}
