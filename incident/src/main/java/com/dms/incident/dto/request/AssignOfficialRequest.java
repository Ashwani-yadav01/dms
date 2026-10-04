package com.dms.incident.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class AssignOfficialRequest {
    @NotNull private UUID officialId;
}
