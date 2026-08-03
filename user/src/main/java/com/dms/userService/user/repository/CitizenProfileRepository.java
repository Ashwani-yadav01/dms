package com.dms.userService.user.repository;

import com.dms.userService.user.entity.CitizenProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface CitizenProfileRepository extends JpaRepository<CitizenProfile, UUID> {
}
