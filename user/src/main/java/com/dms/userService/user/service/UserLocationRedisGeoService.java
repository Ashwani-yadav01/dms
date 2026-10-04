package com.dms.userService.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Redis GEO cache of profile coordinates; PostgreSQL remains authoritative for user data. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLocationRedisGeoService {
    private static final String GEO_KEY_USERS = "users:locations";

    private final StringRedisTemplate redisTemplate;

    public void registerOrUpdateUserLocation(UUID userId, double latitude, double longitude) {
        redisTemplate.opsForGeo().add(GEO_KEY_USERS, new Point(longitude, latitude), userId.toString());
        log.debug("Registered/updated user {} in Redis GEO at ({}, {})", userId, latitude, longitude);
    }

    public void removeUserLocation(UUID userId) {
        redisTemplate.opsForGeo().remove(GEO_KEY_USERS, userId.toString());
        log.debug("Removed user {} from Redis GEO", userId);
    }

    public List<UUID> findUserIdsWithinRadius(double latitude, double longitude, double radiusKm) {
        Circle circle = new Circle(new Point(longitude, latitude), new Distance(radiusKm, Metrics.KILOMETERS));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .sortAscending();
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(GEO_KEY_USERS, circle, args);

        List<UUID> userIds = new ArrayList<>();
        if (results != null) {
            results.forEach(result -> userIds.add(UUID.fromString(result.getContent().getName())));
        }
        log.debug("Found {} users within {} km of ({}, {})", userIds.size(), radiusKm, latitude, longitude);
        return userIds;
    }
}
