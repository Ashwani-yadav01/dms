package com.dms.notificationService.dto.event;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplyDispatchedEvent {
    private UUID dispatchId;
    private String receiverEmail;
    private String itemType;
    private Integer quantity;
    private String vehicleNumber;
    private String warehouseName;
    private String estimatedArrival;
    private LocalDateTime timestamp;
}