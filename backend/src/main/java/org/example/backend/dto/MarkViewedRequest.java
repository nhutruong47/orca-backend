package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.UUID;

public class MarkViewedRequest {
    @NotEmpty(message = "Order IDs list cannot be empty")
    private List<UUID> orderIds;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "BUYER|SELLER",
            message = "Role must be BUYER or SELLER")
    private String role;

    public List<UUID> getOrderIds() { return orderIds; }
    public void setOrderIds(List<UUID> orderIds) { this.orderIds = orderIds; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
