package com.dms.userService.user.controller;

import com.dms.userService.user.dto.response.GovernmentOfficialProfileResponse;
import com.dms.userService.user.entity.DepartmentCategory;
import com.dms.userService.user.entity.HierarchyLevel;
import com.dms.userService.user.entity.OfficialStatus;
import com.dms.userService.user.service.GovernmentOfficialDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/officials")
@RequiredArgsConstructor
public class GovernmentOfficialController {

    private final GovernmentOfficialDomainService officialDomainService;

    // --- SEARCH & HIERARCHY QUERIES ---
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<GovernmentOfficialProfileResponse> getOfficialByEmployeeId(@PathVariable String employeeId) {
        return ResponseEntity.ok(officialDomainService.getOfficialByEmployeeId(employeeId));
    }

    @GetMapping("/department")
    public ResponseEntity<List<GovernmentOfficialProfileResponse>> getOfficialsByDepartment(@RequestParam String name) {
        return ResponseEntity.ok(officialDomainService.getOfficialsByDepartmentName(name));
    }

    @GetMapping("/hierarchy/{level}")
    public ResponseEntity<List<GovernmentOfficialProfileResponse>> getOfficialsByHierarchy(@PathVariable HierarchyLevel level) {
        return ResponseEntity.ok(officialDomainService.getOfficialsByHierarchy(level));
    }

    @GetMapping("/{userId}/supervisor")
    public ResponseEntity<GovernmentOfficialProfileResponse> getSupervisor(@PathVariable UUID userId) {
        return ResponseEntity.ok(officialDomainService.getSupervisor(userId));
    }

    @GetMapping("/{userId}/subordinates")
    public ResponseEntity<List<GovernmentOfficialProfileResponse>> getSubordinates(@PathVariable UUID userId) {
        return ResponseEntity.ok(officialDomainService.getSubordinates(userId));
    }

    @GetMapping("/departments/suggestions")
    public ResponseEntity<List<String>> getDepartmentSuggestions() {
        return ResponseEntity.ok(officialDomainService.getDepartmentSuggestions());
    }

    // --- STATUS & VERIFICATION ---
    @PatchMapping("/{userId}/status")
    public ResponseEntity<GovernmentOfficialProfileResponse> updateStatus(
            @PathVariable UUID userId,
            @RequestParam OfficialStatus status) {
        return ResponseEntity.ok(officialDomainService.updateOfficialStatus(userId, status));
    }

    @PatchMapping("/{userId}/verify")
    public ResponseEntity<GovernmentOfficialProfileResponse> verifyOfficial(
            @PathVariable UUID userId,
            @RequestParam boolean isVerified) {
        return ResponseEntity.ok(officialDomainService.verifyOfficial(userId, isVerified));
    }

    // --- ALLOCATION ELIGIBILITY LOOKUP ---
    @GetMapping("/eligible")
    public ResponseEntity<List<GovernmentOfficialProfileResponse>> findEligibleOfficials(
            @RequestParam OfficialStatus status,
            @RequestParam DepartmentCategory category,
            @RequestParam HierarchyLevel level) {
        return ResponseEntity.ok(officialDomainService.findEligibleOfficials(status, category, level));
    }
}