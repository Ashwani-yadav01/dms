package com.dms.userService.user.entity;

public enum Role {
    CITIZEN,
    VOLUNTEER,
    NGO,
    GOVERNMENT_OFFICIAL,
    SUPER_ADMIN,
    DISTRICT_ADMIN,     // Administrative authority for registering rescue departments & assigning staff
    RESCUE_TEAM,        // On-ground station team members and unit leaders
    HOSPITAL            // Hospital administrator/staff; must be invited by Super Admin and approved through the hospital registration workflow
}
