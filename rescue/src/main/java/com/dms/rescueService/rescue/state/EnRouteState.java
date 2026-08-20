package com.dms.rescueService.rescue.state;

import com.dms.rescueService.rescue.entity.MissionStatus;

public class EnRouteState implements MissionState {
    private static final double GEOFENCE_RADIUS_METERS = 100.0;

    @Override
    public MissionStatus getStatus() {
        return MissionStatus.EN_ROUTE;
    }

    @Override
    public MissionState handleLocationTick(double distanceMeters) {
        if (distanceMeters <= GEOFENCE_RADIUS_METERS) {
            return new OnSceneState(); // Inside geofence -> flip to ON_SCENE
        }
        return this;
    }

    @Override
    public MissionState escalate() {
        return new EscalatedState();
    }

    @Override
    public MissionState cancel() {
        return new CancelledState();
    }
}