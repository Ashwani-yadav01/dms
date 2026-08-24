package com.dms.hospitalService.hospital.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VictimsExtractedEvent {
    private UUID missionId;
    private UUID incidentId;
    private Integer totalVictims;
    // We can use this to prepare emergency rooms in advance
}