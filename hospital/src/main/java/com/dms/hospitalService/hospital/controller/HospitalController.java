package com.dms.hospitalService.hospital.controller;

import com.dms.hospitalService.hospital.dto.request.HospitalCreateRequest;
import com.dms.hospitalService.hospital.dto.response.HospitalResponse;
import com.dms.hospitalService.hospital.entity.MedicalSpeciality;
import com.dms.hospitalService.hospital.service.HospitalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    @PostMapping
    public ResponseEntity<HospitalResponse> createHospital(@Valid @RequestBody HospitalCreateRequest request) {
        return new ResponseEntity<>(hospitalService.createHospital(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HospitalResponse> getHospitalById(@PathVariable UUID id) {
        return ResponseEntity.ok(hospitalService.getHospitalById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<HospitalResponse> toggleAcceptingPatients(
            @PathVariable UUID id,
            @RequestParam Boolean isAccepting) {
        return ResponseEntity.ok(hospitalService.toggleAcceptingPatients(id, isAccepting));
    }

    // 🚨 The Smart Routing Endpoint
    @GetMapping("/nearest")
    public ResponseEntity<List<HospitalResponse>> findNearestCapableHospitals(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "50.0") Double radiusKm,
            @RequestParam(required = false) Set<MedicalSpeciality> specialities,
            @RequestParam(defaultValue = "false") Boolean requiresIcu) {

        List<HospitalResponse> nearest = hospitalService.findNearestCapableHospitals(
                latitude, longitude, radiusKm, specialities, requiresIcu);
        return ResponseEntity.ok(nearest);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHospital(@PathVariable UUID id) {
        hospitalService.deleteHospital(id);
        return ResponseEntity.noContent().build();
    }
}