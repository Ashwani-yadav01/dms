package com.dms.notificationService.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class HospitalSurgeStandbyEvent {
    private String hospitalId;
    private String hospitalPhoneNumber;
    private String disasterType;
    private Double distanceKm;
}