package org.example.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class OrderEventLogDTO {
    private UUID id;
    private UUID orderId;
    private String actorName;
    private String actorRole;
    private String eventType;
    private String oldStatus;
    private String newStatus;
    private String note;
    private String metadata;
    private LocalDateTime createdAt;
}
