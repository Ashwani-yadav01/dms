package com.dms.incident.entity;

public enum LocationTrustLevel {
    MATCH,                  // Device GPS matches incident location (<= 500m)
    MINOR_MISMATCH,         // Device GPS is slightly offset (500m to 5km)
    LARGE_MISMATCH,         // Device GPS is significantly far (> 5km) - flagged for official verification
    REMOTE_REPORT,          // Explicitly reported on behalf of another person / remote victim
    UNVERIFIED              // Device GPS was not provided or could not be verified
}
