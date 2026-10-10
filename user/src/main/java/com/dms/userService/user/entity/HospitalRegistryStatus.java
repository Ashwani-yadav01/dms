package com.dms.userService.user.entity;

/**
 * Lifecycle state for a hospital invitation issued by the Super Admin.
 * A hospital account can only be registered using an ACTIVE invitation code.
 */
public enum HospitalRegistryStatus {
    ACTIVE,    // Invitation issued and not yet consumed; registration code is valid
    CONSUMED,  // Hospital user has registered with this code; code is now single-use
    REVOKED,   // Super Admin revoked the invitation before it was used
    EXPIRED     // Invitation passed its validity window without being used
}