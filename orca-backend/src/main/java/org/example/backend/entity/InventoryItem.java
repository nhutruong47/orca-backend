package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private Double quantity = 0.0;

    @Column(length = 50)
    private String unit; // e.g. "kg", "cái", "hộp"

    @Column(name = "low_stock_threshold")
    private Double lowStockThreshold = 10.0;

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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Double lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
