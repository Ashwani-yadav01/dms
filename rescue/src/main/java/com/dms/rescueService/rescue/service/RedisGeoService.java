package com.dms.rescueService.rescue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisGeoService {

    private static final String KEY_RESCUE_UNITS = "rescue_units";
    private static final String KEY_INCIDENTS = "active_incidents";
    private static final String KEY_MISSION_STATUS_PREFIX = "mission_status:";

    private final StringRedisTemplate redisTemplate;

    public void cacheMissionStatus(UUID missionId, String status) {
        String redisKey = KEY_MISSION_STATUS_PREFIX + missionId.toString();
        redisTemplate.opsForValue().set(redisKey, status);
        log.debug("Cached mission status in Redis -> Key: [{}], Status: [{}]", redisKey, status);
    }

    public String getCachedMissionStatus(UUID missionId) {
        String redisKey = KEY_MISSION_STATUS_PREFIX + missionId.toString();
        String status = redisTemplate.opsForValue().get(redisKey);
        return status != null ? status : "UNKNOWN";
    }

    public void updateUnitLocation(UUID unitId, double latitude, double longitude) {
        // Point constructor takes (X, Y) -> (longitude, latitude)
        redisTemplate.opsForGeo().add(KEY_RESCUE_UNITS, new Point(longitude, latitude), unitId.toString());
        log.debug("Updated Redis GEO position for Unit/Mission ID: [{}] -> ({}, {})", unitId, latitude, longitude);
    }

    public void registerIncidentLocation(UUID incidentId, double latitude, double longitude) {
        // Point constructor takes (X, Y) -> (longitude, latitude)
        redisTemplate.opsForGeo().add(KEY_INCIDENTS, new Point(longitude, latitude), incidentId.toString());
        log.debug("Registered Incident location in Redis GEO for ID: [{}] -> ({}, {})", incidentId, latitude, longitude);
    }

    public double getDistanceToIncidentInMeters(UUID unitId, UUID incidentId) {
        List<Point> unitPos = redisTemplate.opsForGeo().position(KEY_RESCUE_UNITS, unitId.toString());
        List<Point> incidentPos = redisTemplate.opsForGeo().position(KEY_INCIDENTS, incidentId.toString());

        // Fixed NPE safeguard when list contains null
        if (unitPos == null || unitPos.isEmpty() || unitPos.get(0) == null ||
                incidentPos == null || incidentPos.isEmpty() || incidentPos.get(0) == null) {
            return Double.MAX_VALUE;
        }

        Point p1 = unitPos.get(0);
        Point p2 = incidentPos.get(0);

        return calculateHaversineMeters(p1.getY(), p1.getX(), p2.getY(), p2.getX());
    }

    public void removeSpatialData(UUID unitId, UUID incidentId) {
        redisTemplate.opsForZSet().remove(KEY_RESCUE_UNITS, unitId.toString());
        redisTemplate.opsForZSet().remove(KEY_INCIDENTS, incidentId.toString());
        // Preserved mission_status key so REST queries can still read final ON_SCENE state
        log.info("Evicted Redis tracking spatial points for Mission ID: [{}]", unitId);
    }

    private double calculateHaversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_METERS = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}