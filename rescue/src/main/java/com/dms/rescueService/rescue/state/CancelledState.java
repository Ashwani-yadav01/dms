package com.dms.rescueService.rescue.state;

import com.dms.rescueService.rescue.entity.MissionStatus;

public class CancelledState implements MissionState {
    @Override public MissionStatus getStatus() { return MissionStatus.CANCELLED; }
}