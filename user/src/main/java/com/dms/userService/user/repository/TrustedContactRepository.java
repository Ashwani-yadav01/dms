package com.dms.userService.user.repository;

import com.dms.userService.user.entity.TrustedContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrustedContactRepository extends JpaRepository<TrustedContact, UUID> {
    List<TrustedContact> findByUserIdOrderByCreatedAtAsc(UUID userId);
    Optional<TrustedContact> findByIdAndUserId(UUID id, UUID userId);
    long countByUserId(UUID userId);
}
