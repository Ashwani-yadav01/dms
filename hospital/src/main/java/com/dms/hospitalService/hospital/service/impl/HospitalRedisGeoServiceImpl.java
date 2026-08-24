package com.dms.hospitalService.hospital.service.impl;

import com.dms.hospitalService.hospital.service.HospitalRedisGeoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalRedisGeoServiceImpl implements HospitalRedisGeoService {

    private final RedisTemplate<String, String> redisTemplate;

    // The key used in Redis to store the sorted spatial set of hospitals
    private static final String GEO_KEY_HOSPITALS = "hospitals:locations";

    @Override
    public void registerHospitalLocation(UUID hospitalId, double latitude, double longitude) {
        Point point = new Point(longitude, latitude); // Redis expects Longitude first
        redisTemplate.opsForGeo().add(GEO_KEY_HOSPITALS, point, hospitalId.toString());
        log.info("Registered/Updated Hospital ID: {} in Redis GEO at ({}, {})", hospitalId, latitude, longitude);
    }

    @Override
    public void removeHospitalLocation(UUID hospitalId) {
        redisTemplate.opsForGeo().remove(GEO_KEY_HOSPITALS, hospitalId.toString());
        log.info("Removed Hospital ID: {} from Redis GEO cache", hospitalId);
    }

    @Override
    public List<UUID> findHospitalsWithinRadius(double latitude, double longitude, double radiusKm) {
        Point center = new Point(longitude, latitude);
        Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);
        Circle circle = new Circle(center, radius);

        // Define arguments for GEO search (sort by distance)
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .sortAscending();

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(GEO_KEY_HOSPITALS, circle, args);

        List<UUID> hospitalIds = new ArrayList<>();
        if (results != null) {
            results.forEach(result -> {
                String idStr = result.getContent().getName();
                hospitalIds.add(UUID.fromString(idStr));
            });
        }

        log.debug("Found {} hospitals within {} km of ({}, {})", hospitalIds.size(), radiusKm, latitude, longitude);
        return hospitalIds;
    }
}