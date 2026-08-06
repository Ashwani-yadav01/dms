package com.dms.incident.entity;

import lombok.Getter;

@Getter
public enum Severity {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    // Optional priority rank for sorting algorithms or database ordering
    private final int priority;

    Severity(int priority) {
        this.priority = priority;
    }
}