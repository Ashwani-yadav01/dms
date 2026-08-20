package com.dms.rescueService.rescue.state;

import com.dms.rescueService.rescue.entity.MissionStatus;

// DISPATCHED STATE
public class DispatchedState implements MissionState {
    @Override
    public MissionStatus getStatus() {
        return MissionStatus.DISPATCHED;
    }

    @Override
    public MissionState handleLocationTick(double distanceMeters) {
        return new EnRouteState(); // First motion flips status to EN_ROUTE
    }

    @Override
    public MissionState cancel() {
        return new CancelledState();
    }
}

// EN_ROUTE STATE



