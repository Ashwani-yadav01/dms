package com.dms.incident.entity;

public enum IncidentType {
    FLOOD,       // Submerged areas, flash floods, tsunamis -> Needs Water/Boat Team
    FIRE,        // Building fires, forest fires, gas leaks -> Needs Fire Brigade
    COLLAPSE,    // Building collapse, landslides, earthquakes -> Needs Heavy Extrication/USAR
    MEDICAL,     // Mass casualties, road/train accidents -> Needs Ambulances/EMTs
    HAZMAT,      // Chemical spills, toxic gas, radiation -> Needs Specialized Hazmat Team
    OTHER        // General distress, power outage, fallen trees -> Needs General Volunteers
}
