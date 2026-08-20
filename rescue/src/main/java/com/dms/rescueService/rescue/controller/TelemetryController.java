package com.dms.rescueService.rescue.controller;

import com.dms.rescueService.rescue.dto.request.TelemetryPingRequest;
import com.dms.rescueService.rescue.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rescue/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService telemetryService;

    @PostMapping("/ping")
    public ResponseEntity<String> updateLocation(@RequestBody TelemetryPingRequest request) {
        telemetryService.processTelemetryPing(
                request.getMissionId(),
                request.getLatitude(),
                request.getLongitude()
        );
        return ResponseEntity.ok("Telemetry processed and location updated.");
    }
}