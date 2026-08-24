package com.dms.hospitalService.hospital.service;

import java.util.List;
import java.util.UUID;

public interface HospitalRedisGeoService {
    void registerHospitalLocation(UUID hospitalId, double latitude, double longitude);
    void removeHospitalLocation(UUID hospitalId);
    List<UUID> findHospitalsWithinRadius(double latitude, double longitude, double radiusKm);
}