package com.dms.hospitalService.hospital.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VictimsExtractedEvent implements Serializable {
    private UUID missionId;
    private UUID incidentId;
    private Integer totalVictims;
}
