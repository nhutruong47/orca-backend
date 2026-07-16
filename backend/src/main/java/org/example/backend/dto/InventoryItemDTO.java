package org.example.backend.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class InventoryItemDTO {
    private String id;
    
    @NotBlank(message = "Team ID is required")
    private String teamId;
    
    @NotBlank(message = "Product Type is required")
    private String productType;
    
    private String productState;
    
    @NotBlank(message = "Display Name is required")
    private String displayName;
    
    @NotNull(message = "Quantity is required")
    private Double quantity;
    
    @NotBlank(message = "Unit is required")
    private String unit;
    private Double lowStockThreshold;
    private String status;
    private LocalDateTime lastUpdated;

    // Featured Product fields
    private String price;
    private String description;
    private String imageUrl;
    private String origin;
    private String roastLevel;
    private String processing;
    private String tasteNotes;
    private Boolean isFeatured;

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

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getRoastLevel() { return roastLevel; }
    public void setRoastLevel(String roastLevel) { this.roastLevel = roastLevel; }

    public String getProcessing() { return processing; }
    public void setProcessing(String processing) { this.processing = processing; }

    public String getTasteNotes() { return tasteNotes; }
    public void setTasteNotes(String tasteNotes) { this.tasteNotes = tasteNotes; }

    public Boolean getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Boolean isFeatured) { this.isFeatured = isFeatured; }
}
