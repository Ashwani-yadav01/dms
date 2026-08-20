package com.dms.rescueService.rescue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisGeoService {

    private static final String KEY_RESCUE_UNITS = "rescue_units";
    private static final String KEY_INCIDENTS = "active_incidents";

    private final StringRedisTemplate redisTemplate;

    public void updateUnitLocation(UUID unitId, double longitude, double latitude) {
        redisTemplate.opsForGeo().add(KEY_RESCUE_UNITS, new Point(longitude, latitude), unitId.toString());
    }

    public void registerIncidentLocation(UUID incidentId, double longitude, double latitude) {
        redisTemplate.opsForGeo().add(KEY_INCIDENTS, new Point(longitude, latitude), incidentId.toString());
    }

    public double getDistanceToIncidentInMeters(UUID unitId, UUID incidentId) {
        List<Point> unitPos = redisTemplate.opsForGeo().position(KEY_RESCUE_UNITS, unitId.toString());
        List<Point> incidentPos = redisTemplate.opsForGeo().position(KEY_INCIDENTS, incidentId.toString());

        if (unitPos == null || incidentPos == null || unitPos.get(0) == null || incidentPos.get(0) == null) {
            return Double.MAX_VALUE;
        }

        Point p1 = unitPos.get(0);
        Point p2 = incidentPos.get(0);
        return calculateHaversineMeters(p1.getY(), p1.getX(), p2.getY(), p2.getX());
    }

    public void removeSpatialData(UUID unitId, UUID incidentId) {
        redisTemplate.opsForZSet().remove(KEY_RESCUE_UNITS, unitId.toString());
        redisTemplate.opsForZSet().remove(KEY_INCIDENTS, incidentId.toString());
    }

    private double calculateHaversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}