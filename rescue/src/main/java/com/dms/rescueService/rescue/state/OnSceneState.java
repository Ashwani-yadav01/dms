package com.dms.rescueService.rescue.state;

import com.dms.rescueService.rescue.entity.MissionStatus;

// ON_SCENE STATE
public class OnSceneState implements MissionState {
    @Override
    public MissionStatus getStatus() {
        return MissionStatus.ON_SCENE;
    }

    @Override
    public MissionState handleLocationTick(double distanceMeters) {
        return this; // Keep state ON_SCENE
    }

    @Override
    public MissionState complete() {
        return new CompletedState();
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