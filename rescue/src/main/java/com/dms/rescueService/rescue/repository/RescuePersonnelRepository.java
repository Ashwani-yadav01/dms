package com.dms.rescueService.rescue.repository;

import com.dms.rescueService.rescue.entity.RescuePersonnel;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface RescuePersonnelRepository extends JpaRepository<RescuePersonnel, UUID> {

    // Finds the first available Chief for the assigned department

    @Query("""
        SELECT p FROM RescuePersonnel p 
        WHERE p.department.id = :deptId 
          AND p.isChief = true 
          AND p.isAvailable = true
    """)
    List<RescuePersonnel> findAvailableChiefs(@Param("deptId") UUID deptId);
}