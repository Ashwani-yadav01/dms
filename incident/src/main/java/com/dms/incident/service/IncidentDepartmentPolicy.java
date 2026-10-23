package com.dms.incident.service;

import com.dms.incident.entity.IncidentType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class IncidentDepartmentPolicy {

    private static final Map<IncidentType, Set<String>> TYPE_TO_DEPARTMENTS = new EnumMap<>(IncidentType.class);

    static {
        TYPE_TO_DEPARTMENTS.put(IncidentType.FIRE, Set.of("FIRE_SAFETY", "DEPARTMENT_MANAGEMENT"));
        TYPE_TO_DEPARTMENTS.put(IncidentType.HAZMAT, Set.of("FIRE_SAFETY", "DEPARTMENT_MANAGEMENT"));
        TYPE_TO_DEPARTMENTS.put(IncidentType.FLOOD, Set.of("DISASTER_MANAGEMENT", "DEPARTMENT_MANAGEMENT"));
        TYPE_TO_DEPARTMENTS.put(IncidentType.COLLAPSE, Set.of("DISASTER_MANAGEMENT", "DEPARTMENT_MANAGEMENT"));
        TYPE_TO_DEPARTMENTS.put(IncidentType.ACCIDENT, Set.of("ACCIDENT", "DEPARTMENT_MANAGEMENT"));
        TYPE_TO_DEPARTMENTS.put(IncidentType.MEDICAL, Set.of("MEDICAL_RESPONSE", "DEPARTMENT_MANAGEMENT"));
        TYPE_TO_DEPARTMENTS.put(IncidentType.OTHER, Set.of("DISASTER_MANAGEMENT", "DEPARTMENT_MANAGEMENT"));
    }

    private IncidentDepartmentPolicy() {}

    public static Set<String> getAuthorizedDepartmentCategories(IncidentType type) {
        if (type == null) {
            return Collections.emptySet();
        }
        return TYPE_TO_DEPARTMENTS.getOrDefault(type, Collections.emptySet());
    }

    public static Set<IncidentType> getAuthorizedIncidentTypes(String departmentCategory) {
        if (departmentCategory == null || departmentCategory.isBlank()) {
            return Collections.emptySet();
        }
        Set<IncidentType> types = EnumSet.noneOf(IncidentType.class);
        for (Map.Entry<IncidentType, Set<String>> entry : TYPE_TO_DEPARTMENTS.entrySet()) {
            if (entry.getValue().contains(departmentCategory)) {
                types.add(entry.getKey());
            }
        }
        return types;
    }

    public static boolean isAuthorized(String departmentCategory, IncidentType type) {
        if (departmentCategory == null || type == null) {
            return false;
        }
        Set<String> categories = TYPE_TO_DEPARTMENTS.get(type);
        return categories != null && categories.contains(departmentCategory);
    }
}
