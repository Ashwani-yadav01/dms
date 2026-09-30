package com.dms.incident.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class IncidentRejectionRequest {
    @NotBlank(message = "A reason is required to reject an incident")
    @Size(max = 5000, message = "The rejection reason cannot exceed 5000 characters")
    private String reason;
}
