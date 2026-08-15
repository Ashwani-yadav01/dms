package com.dms.incident.dto.request;

import com.dms.incident.entity.IncidentType;
import com.dms.incident.entity.Severity;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class IncidentRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    private Double longitude;

    @NotNull(message = "Severity level is required")
    private Severity severity;

    @NotNull(message = "Incident type is required for rescue team allocation")
    private IncidentType incidentType;

    @NotBlank(message = "Please provide an image URL of the incident to assist rescue operations")
    private String imageUrl;
}