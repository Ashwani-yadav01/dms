package com.dms.notificationService.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class EvacuationAlertEvent {
    private String disasterType; // e.g., "CHEMICAL_SPILL"
    private List<String> targetPhoneNumbers;
    private String hazardZoneName;
}
