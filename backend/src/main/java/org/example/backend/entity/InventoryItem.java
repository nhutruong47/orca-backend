package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "product_type", "product_state"}))
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /** ARABICA, ROBUSTA, CULI, BLEND or custom name */
    @Column(name = "product_type", length = 100)
    private String productType;

    /** GREEN (hạt xanh), ROASTED (đã rang), GROUND (đã xay), PACKAGED (đã đóng gói) */
    @Column(name = "product_state", length = 30)
    private String productState = "GREEN";

    @Column
    private Double quantity = 0.0;

    @Column(length = 50)
    private String unit = "kg";

    @Column(name = "low_stock_threshold")
    private Double lowStockThreshold = 100.0;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    // Computed property (not mapped) for frontend ease
    public String getStockStatus() {
        if (this.quantity <= 0) return "OUT_OF_STOCK";
        if (this.quantity <= this.lowStockThreshold) return "LOW_STOCK";
        return "IN_STOCK";
    }

    /** Convenience: human-readable name combining type + state */
    public String getDisplayName() {
        String stateVi = switch (productState) {
            case "GREEN" -> "Hạt xanh";
            case "ROASTED" -> "Đã rang";
            case "GROUND" -> "Đã xay";
            case "PACKAGED" -> "Đã đóng gói";
            default -> productState;
        };
        return productType + " - " + stateVi;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getProductState() { return productState; }
    public void setProductState(String productState) { this.productState = productState; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Double getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Double lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    /** @deprecated Use productType instead */
    @Transient
    public String getName() {
        return getDisplayName();
    }
}
