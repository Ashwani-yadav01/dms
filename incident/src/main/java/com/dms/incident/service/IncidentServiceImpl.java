package com.dms.incident.service;

import com.dms.incident.dto.request.IncidentRequest;
import com.dms.incident.dto.response.IncidentResponse;
import com.dms.incident.entity.Incident;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;
@RequiredArgsConstructor
@Service
public class IncidentServiceImpl implements IncidentService{
    private final ModelMapper mapper;

    @Override
    public IncidentResponse reportIncident(IncidentRequest request, UUID id) {
        return null;
    }

    @Override
    public IncidentResponse getIncidentById(UUID id) {
        return null;
    }
}
