package com.dms.rescueService.rescue.controller;

import com.dms.rescueService.rescue.dto.request.DepartmentCreateRequest;
import com.dms.rescueService.rescue.dto.request.DepartmentUpdateRequest;
import com.dms.rescueService.rescue.dto.request.StationChiefRegisterRequest;
import com.dms.rescueService.rescue.dto.response.DepartmentResponse;
import com.dms.rescueService.rescue.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rescue/departments")
@RequiredArgsConstructor
public class RescueDepartmentController {

    private final DepartmentService departmentService;

    // --- CREATE ---
    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentCreateRequest request) {
        return new ResponseEntity<>(departmentService.createDepartment(request), HttpStatus.CREATED);
    }

    // --- READ ALL / READ SINGLE ---
    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    // --- READ BY JURISDICTION ---
    @GetMapping("/jurisdiction/{jurisdictionCode}")
    public ResponseEntity<List<DepartmentResponse>> getByJurisdiction(@PathVariable String jurisdictionCode) {
        return ResponseEntity.ok(departmentService.getDepartmentsByJurisdiction(jurisdictionCode));
    }

    // --- NEARBY SPATIAL SEARCH ---
    @GetMapping("/nearby")
    public ResponseEntity<List<DepartmentResponse>> findNearby(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "25.0") Double radiusKm) {
        return ResponseEntity.ok(departmentService.findNearbyDepartments(latitude, longitude, radiusKm));
    }

    // --- FULL / PARTIAL UPDATE ---
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentUpdateRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    // --- QUICK TOGGLE AVAILABILITY ---
    @PatchMapping("/{id}/availability")
    public ResponseEntity<DepartmentResponse> toggleAvailability(
            @PathVariable UUID id,
            @RequestParam Boolean isAvailable) {
        return ResponseEntity.ok(departmentService.toggleAvailability(id, isAvailable));
    }

    // --- DELETE ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable UUID id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    // --- REGISTER STATION CHIEF ---
    @PostMapping("/{id}/chiefs")
    public ResponseEntity<DepartmentResponse> registerStationChief(
            @PathVariable UUID id,
            @Valid @RequestBody StationChiefRegisterRequest request) {
        return new ResponseEntity<>(departmentService.registerStationChief(id, request), HttpStatus.CREATED);
    }
}