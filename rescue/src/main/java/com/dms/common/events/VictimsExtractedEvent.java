package com.dms.common.events;

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
}