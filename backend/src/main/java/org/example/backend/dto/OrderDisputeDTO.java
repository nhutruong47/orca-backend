package org.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDisputeDTO {
    private UUID id;
    private UUID orderId;
    private UUID openedByUserId;
    private String reason;
    private String evidenceUrls;
    private BigDecimal compensationAmount;
    private String status;
    private String resolutionNote;
    private UUID resolvedByUserId;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
