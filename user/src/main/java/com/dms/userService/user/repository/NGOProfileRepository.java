package com.dms.userService.user.repository;

import com.dms.userService.user.entity.NGOProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NGOProfileRepository extends JpaRepository<NGOProfile, UUID> {
}
