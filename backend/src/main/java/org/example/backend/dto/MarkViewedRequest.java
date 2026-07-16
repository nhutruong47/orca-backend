package org.example.backend.dto;

import java.util.List;
import java.util.UUID;

public class MarkViewedRequest {
    private List<UUID> orderIds;
    private String role;

    public List<UUID> getOrderIds() {
        return orderIds;
    }

    public void setOrderIds(List<UUID> orderIds) {
        this.orderIds = orderIds;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
