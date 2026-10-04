package com.dms.userService.user.repository;

import com.dms.userService.user.entity.RescueTeamProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RescueTeamProfileRepository extends JpaRepository<RescueTeamProfile, UUID> {
    boolean existsByTeamCode(String teamCode);
}
