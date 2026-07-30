package com.dms.incident.service;

import com.dms.incident.dto.request.IncidentRequest;
import com.dms.incident.dto.response.IncidentResponse;

import java.util.UUID;

public interface IncidentService {
    IncidentResponse reportIncident(IncidentRequest request, UUID id);
    IncidentResponse getIncidentById(UUID id);
}
