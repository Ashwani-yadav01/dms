package com.dms.userService.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SosAlertRequest {

    @NotBlank(message = "Client reference ID is required for idempotency")
    private String clientReferenceId;

    @Size(max = 500, message = "Emergency message must not exceed 500 characters")
    private String message;

    private Double latitude;

    private Double longitude;
}
