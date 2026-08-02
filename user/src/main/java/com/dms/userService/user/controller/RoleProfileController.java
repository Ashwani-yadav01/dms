package com.dms.userService.user.controller;

import com.dms.userService.user.dto.response.*;
import com.dms.userService.user.dto.request.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/users/{userId}/roles")
public class RoleProfileController {

    // ---------------- Volunteer ----------------

    @PostMapping("/volunteer")
    public ResponseEntity<VolunteerProfileResponse> createVolunteer(
            @RequestBody VolunteerProfileRequest request) {
        return null;
    }

    @GetMapping("/volunteer/{userId}")
    public ResponseEntity<VolunteerProfileResponse> getVolunteer(
            @PathVariable UUID userId) {
        return null;
    }

    @PutMapping("/volunteer")
    public ResponseEntity<VolunteerProfileResponse> updateVolunteer(
            @PathVariable UUID userId,
            @RequestBody VolunteerProfileRequest request) {
        return null;
    }

    @DeleteMapping("/volunteer")
    public ResponseEntity<Void> deleteVolunteer(
            @PathVariable UUID userId) {
        return null;
    }

    // ---------------- NGO ----------------

    @PostMapping("/ngo")
    public ResponseEntity<NGOProfileResponse> createNGO(
            @RequestBody NGOProfileRequest request) {
        return null;
    }

    @GetMapping("/ngo/{userId}")
    public ResponseEntity<NGOProfileResponse> getNGO(
            @PathVariable UUID userId) {
        return null;
    }

    @PutMapping("/ngo/{userId}")
    public ResponseEntity<NGOProfileResponse> updateNGO(
            @PathVariable UUID userId,
            @RequestBody NGOProfileRequest request) {
        return null;
    }

    @DeleteMapping("/ngo/{userId}")
    public ResponseEntity<Void> deleteNGO(
            @PathVariable UUID userId) {
        return null;
    }

    // ---------------- Government Official ----------------

    @PostMapping("/government")
    public ResponseEntity<GovernmentOfficialProfileResponse> createOfficial(
            @RequestBody GovernmentOfficialProfileRequest request) {
        return null;
    }

    @GetMapping("/government/{userId}")
    public ResponseEntity<GovernmentOfficialProfileResponse> getOfficial(
            @PathVariable UUID userId) {
        return null;
    }

    @PutMapping("/government/{userId}")
    public ResponseEntity<GovernmentOfficialProfileResponse> updateOfficial(
            @PathVariable UUID userId,
            @RequestBody GovernmentOfficialProfileResponse request) {
        return null;
    }

    @DeleteMapping("/government/{userId}")
    public ResponseEntity<Void> deleteOfficial(
            @PathVariable UUID userId) {
        return null;
    }

    // ---------------- Departments ----------------

    @PostMapping("/government/departments")
    public ResponseEntity<GovernmentDepartmentResponse> createDepartment(
            @RequestBody GovernmentDepartmentRequest request) {
        return null;
    }

    @GetMapping("/government/departments")
    public ResponseEntity<List<GovernmentDepartmentResponse>> getAllDepartments() {
        return null;
    }

    @GetMapping("/government/departments/{departmentId}")
    public ResponseEntity<GovernmentDepartmentResponse> getDepartment(
            @PathVariable UUID departmentId) {
        return null;
    }

    @PutMapping("/government/departments/{departmentId}")
    public ResponseEntity<GovernmentDepartmentResponse> updateDepartment(
            @PathVariable UUID departmentId,
            @RequestBody GovernmentDepartmentRequest request) {
        return null;
    }

    @DeleteMapping("/government/departments/{departmentId}")
    public ResponseEntity<Void> deleteDepartment(
            @PathVariable UUID departmentId) {
        return null;
    }
}