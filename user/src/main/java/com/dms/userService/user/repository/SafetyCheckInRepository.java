package com.dms.userService.user.repository;

import com.dms.userService.user.entity.SafetyCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SafetyCheckInRepository extends JpaRepository<SafetyCheckIn, UUID> {
    Optional<SafetyCheckIn> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
    List<SafetyCheckIn> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<SafetyCheckIn> findByClientReferenceId(String clientReferenceId);
}
