package com.dms.rescueService.rescue.entity;

import java.util.Optional;

public enum MissionStatus {
    DISPATCHED,
    EN_ROUTE,
    ON_SCENE,
    COMPLETED,
    ESCALATED,
    CANCELLED;

    public static Optional<MissionStatus> fromString(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        for (MissionStatus status : MissionStatus.values()) {
            if (status.name().equalsIgnoreCase(text.trim())) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}