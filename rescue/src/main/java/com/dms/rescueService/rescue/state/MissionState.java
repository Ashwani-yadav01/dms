package com.dms.rescueService.rescue.state;

import com.dms.rescueService.rescue.entity.MissionStatus;

public interface MissionState {

    MissionStatus getStatus();

    default MissionState handleLocationTick(double distanceToIncidentMeters) {
        throw new IllegalStateException("Location ticks are invalid in status: " + getStatus());
    }

    default MissionState complete() {
        throw new IllegalStateException("Cannot complete mission from status: " + getStatus());
    }

    default MissionState escalate() {
        throw new IllegalStateException("Cannot escalate mission from status: " + getStatus());
    }

    default MissionState cancel() {
        throw new IllegalStateException("Cannot cancel mission from status: " + getStatus());
    }
}