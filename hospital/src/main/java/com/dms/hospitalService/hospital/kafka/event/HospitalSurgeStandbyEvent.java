package com.dms.hospitalService.hospital.kafka.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalSurgeStandbyEvent implements Serializable {
    private UUID incidentId;
    private String hospitalEmail;
    private String hospitalName;
    private String disasterType;
    private Double distanceKm;
    private Integer estimatedCasualties;
    private LocalDateTime timestamp;
}