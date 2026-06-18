package org.example.backend.dto;

import java.time.LocalDateTime;

public class InventoryItemDTO {
    private String id;
    private String teamId;
    private String productType;
    private String productState;
    private String displayName;
    private Double quantity;
    private String unit;
    private Double lowStockThreshold;
    private String status;
    private LocalDateTime lastUpdated;

    // Keep backward compat
    private String name;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getProductState() { return productState; }
    public void setProductState(String productState) { this.productState = productState; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Double getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Double lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
