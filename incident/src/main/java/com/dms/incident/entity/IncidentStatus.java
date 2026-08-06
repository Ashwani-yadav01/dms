package com.dms.incident.entity;

public enum IncidentStatus {
    REPORTED,
    VERIFIED,
    DISPATCHED,
    RESOLVED,
    REJECTED,
    DUPLICATE;
    /**
     * Helper check to determine if an incident is still actively needing attention.
     */
    public boolean isActive() {
        return this == REPORTED || this == VERIFIED || this == DISPATCHED;
    }

    /**
     * Helper check to determine if an incident is closed.
     */
    public boolean isTerminal() {
        return this == RESOLVED || this == REJECTED;
    }
}