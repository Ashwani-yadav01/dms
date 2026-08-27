package com.dms.hospitalService.hospital.controller;

import com.dms.hospitalService.hospital.service.HospitalInventoryAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hospitals")
@RequiredArgsConstructor
public class HospitalInventoryController {

    private final HospitalInventoryAlertService alertService;

    // Example payload for Postman: {"itemType": "BLOOD_O_NEG", "requestedQuantity": 50, "urgencyLevel": "CRITICAL"}
    @PostMapping("/{hospitalId}/inventory/shortage")
    public ResponseEntity<String> declareShortage(
            @PathVariable UUID hospitalId,
            @RequestBody ShortageRequest request) {

        alertService.triggerShortageAlert(
                hospitalId,
                request.itemType(),
                request.requestedQuantity(),
                request.urgencyLevel()
        );

        return ResponseEntity.accepted().body("Shortage alert broadcasted to Logistics Service.");
    }

    // Simple record for the incoming JSON body
    public record ShortageRequest(String itemType, int requestedQuantity, String urgencyLevel) {}
}