package com.dms.hospitalService.hospital.entity;

public enum TriageLevel {
    RED,    // Critical (Immediate surgery/ICU)
    YELLOW, // Severe (Needs urgent care, but stable)
    GREEN,  // Minor (Walking wounded, route to PHC)
    BLACK   // Deceased/Expectant
}