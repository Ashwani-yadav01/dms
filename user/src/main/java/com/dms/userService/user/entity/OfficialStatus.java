package com.dms.userService.user.entity;

public enum OfficialStatus {
    AVAILABLE,     // Ready for auto-allocation
    ON_DUTY,       // Currently assigned to an active emergency
    ON_LEAVE,      // Temporarily unavailable
    UNREACHABLE    // Out of service area
}