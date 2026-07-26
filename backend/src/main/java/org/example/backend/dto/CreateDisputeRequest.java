package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateDisputeRequest {
    @NotNull(message = "Order ID không được để trống")
    private UUID orderId;

    @NotBlank(message = "Lý do khiếu nại không được để trống")
    private String reason;

    private List<String> evidenceUrls;

    private BigDecimal compensationAmount;
}
