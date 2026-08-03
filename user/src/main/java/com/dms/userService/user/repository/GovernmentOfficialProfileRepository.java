package com.dms.userService.user.repository;

import com.dms.userService.user.entity.DepartmentCategory;
import com.dms.userService.user.entity.GovernmentOfficialProfile;
import com.dms.userService.user.entity.HierarchyLevel;
import com.dms.userService.user.entity.OfficialStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GovernmentOfficialProfileRepository extends JpaRepository<GovernmentOfficialProfile, UUID> {

    Optional<GovernmentOfficialProfile> findByEmployeeId(String employeeId);

    boolean existsByEmployeeId(String employeeId);

    List<GovernmentOfficialProfile> findByDepartmentName(String departmentName);

    List<GovernmentOfficialProfile> findByHierarchyLevel(HierarchyLevel hierarchyLevel);

    List<GovernmentOfficialProfile> findByReportsToId(UUID supervisorId);

    // --- AUTO-SUGGESTION QUERY FOR FRONTEND UI ---
    @Query("SELECT DISTINCT g.departmentName FROM GovernmentOfficialProfile g WHERE g.departmentName IS NOT NULL ORDER BY g.departmentName ASC")
    List<String> findDistinctDepartmentNames();

    // --- ALLOCATION ENGINE QUERY ---
    @Query("SELECT g FROM GovernmentOfficialProfile g " +
            "WHERE g.status = :status " +
            "AND g.isVerified = true " +
            "AND g.departmentCategory = :category " +
            "AND (:level IS NULL OR g.hierarchyLevel = :level)")
    List<GovernmentOfficialProfile> findEligibleOfficialsForAllocation(
            @Param("status") OfficialStatus status,
            @Param("category") DepartmentCategory category,
            @Param("level") HierarchyLevel level
    );
}