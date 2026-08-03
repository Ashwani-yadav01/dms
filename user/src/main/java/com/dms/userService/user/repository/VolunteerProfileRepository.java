package com.dms.userService.user.repository;

import com.dms.userService.user.entity.Availability;
import com.dms.userService.user.entity.VolunteerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VolunteerProfileRepository extends JpaRepository<VolunteerProfile, UUID> {

    // 1. Direct Enum Match
    List<VolunteerProfile> findByAvailability(Availability availability);

    // 2. Explicit JPQL for ElementCollection (@ElementCollection List<String> skills)
    @Query("SELECT v FROM VolunteerProfile v WHERE :skill MEMBER OF v.skills")
    List<VolunteerProfile> findBySkill(@Param("skill") String skill);

    // 3. Case-Insensitive Skill Search (Bonus: handles "first_aid" vs "FIRST_AID")
    @Query("SELECT DISTINCT v FROM VolunteerProfile v JOIN v.skills s WHERE LOWER(s) = LOWER(:skill)")
    List<VolunteerProfile> findBySkillIgnoreCase(@Param("skill") String skill);
}