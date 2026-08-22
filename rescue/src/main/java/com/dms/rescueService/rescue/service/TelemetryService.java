package com.dms.rescueService.rescue.service;

import com.dms.common.events.RescueMissionStatusUpdatedEvent;
import com.dms.rescueService.rescue.entity.MissionStatus;
import com.dms.rescueService.rescue.entity.RescueMission;
import com.dms.rescueService.rescue.repository.RescueMissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final RedisGeoService redisGeoService;
    private final RescueMissionRepository missionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.rescue-mission-status:rescue-mission-status-topic}")
    private String rescueStatusTopic;

    private static final double GEOFENCE_ARRIVED_METERS = 50.0;

    @Transactional
    public void processTelemetryPing(UUID missionId, double latitude, double longitude) {
        RescueMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found: " + missionId));

        // 1. Update unit coordinates ONLY in Redis GEO (Does not touch PostgreSQL)
        redisGeoService.updateUnitLocation(missionId, latitude, longitude);

        // 2. First ping transitions DISPATCHED -> EN_ROUTE (Triggers single DB save)
        if (mission.getStatus() == MissionStatus.DISPATCHED) {
            updateMissionStatus(mission, MissionStatus.EN_ROUTE, "Unit moving to scene.");

            // 3. Evaluate 50m geofence while moving
        } else if (mission.getStatus() == MissionStatus.EN_ROUTE) {
            // Correct parameter ordering: (unitId, incidentId)
            double distanceMeters = redisGeoService.getDistanceToIncidentInMeters(missionId, mission.getIncidentId());
            log.info("Mission [{}] distance to incident: {} meters", missionId, distanceMeters);

            // 4. Update PostgreSQL ONLY when unit reaches target geofence radius
            if (distanceMeters <= GEOFENCE_ARRIVED_METERS) {
                mission.setArrivalLatitude(latitude);
                mission.setArrivalLongitude(longitude);
                mission.setArrivedAt(LocalDateTime.now());

                updateMissionStatus(
                        mission,
                        MissionStatus.ON_SCENE,
                        String.format("Arrived within 50m (Persisted: %.4f, %.4f)", latitude, longitude)
                );

                // Clean up Redis spatial entries after arrival
                redisGeoService.removeSpatialData(missionId, mission.getIncidentId());
            }
        }
    }

    private void updateMissionStatus(RescueMission mission, MissionStatus newStatus, String notes) {
        log.info("Transitioning mission [{}] status from {} to {}", mission.getId(), mission.getStatus(), newStatus);

        mission.setStatus(newStatus);
        mission.setNotes(notes);

        // Persist status change to PostgreSQL
        missionRepository.save(mission);

        // Sync status state to Redis cache
        redisGeoService.cacheMissionStatus(mission.getId(), newStatus.name());

        // Publish event to Kafka
        RescueMissionStatusUpdatedEvent event = RescueMissionStatusUpdatedEvent.builder()
                .incidentId(mission.getIncidentId())
                .missionId(mission.getId())
                .status(newStatus)
                .notes(notes)
                .build();

        kafkaTemplate.send(rescueStatusTopic, mission.getIncidentId().toString(), event);
    }
}