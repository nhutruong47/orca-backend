package org.example.backend.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private Double price;
    private String period; // e.g. "Tháng", "Năm"
    
    private Integer maxUsers;
    private Integer maxOrders;
    private Integer maxBatches;
    private Integer maxWorkshops;
    private Integer aiLimit;
    
    @Column(columnDefinition = "TEXT")
    private String features; // Comma separated list of features or JSON array

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }

    public Integer getMaxOrders() { return maxOrders; }
    public void setMaxOrders(Integer maxOrders) { this.maxOrders = maxOrders; }

    public Integer getMaxBatches() { return maxBatches; }
    public void setMaxBatches(Integer maxBatches) { this.maxBatches = maxBatches; }

    public Integer getMaxWorkshops() { return maxWorkshops; }
    public void setMaxWorkshops(Integer maxWorkshops) { this.maxWorkshops = maxWorkshops; }

    public Integer getAiLimit() { return aiLimit; }
    public void setAiLimit(Integer aiLimit) { this.aiLimit = aiLimit; }

    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }
}
