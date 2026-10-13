package com.dms.hospitalService.hospital.entity;

public enum HospitalStatus {
    PENDING_VERIFICATION, // Newly registered, awaits official review & verification
    ACTIVE,               // Hospital is fully operational and visible in emergency routing
    SUSPENDED,            // Temporarily unavailable - not visible in routing, but record preserved
    DEACTIVATED,          // Permanently closed - not visible in routing, preserved for history
    REJECTED              // Rejected by official authorities
}