package com.dms.userService.user.entity;

public enum Role {
    CITIZEN,
    VOLUNTEER,
    NGO,
    GOVERNMENT_OFFICIAL,
    DISTRICT_ADMIN,     // Administrative authority for registering rescue departments & assigning staff
    RESCUE_TEAM         // On-ground station team members and unit leaders
}