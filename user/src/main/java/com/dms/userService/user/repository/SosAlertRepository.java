package com.dms.userService.user.repository;

import com.dms.userService.user.entity.SosAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SosAlertRepository extends JpaRepository<SosAlert, UUID> {
    Optional<SosAlert> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
    List<SosAlert> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<SosAlert> findByClientReferenceId(String clientReferenceId);
}
