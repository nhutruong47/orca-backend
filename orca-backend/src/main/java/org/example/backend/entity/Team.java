package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished = false;

    private String specialty;
    private String capacity;
    private String region;

    @Column(name = "completed_orders", nullable = false)
    private int completedOrders = 0;

    @Column(name = "cancelled_orders", nullable = false)
    private int cancelledOrders = 0;

    @Column(name = "total_orders", nullable = false)
    private int totalOrders = 0;

    @Column(name = "invite_code", unique = true, length = 6)
    private String inviteCode;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.inviteCode == null || this.inviteCode.isEmpty()) {
            this.inviteCode = generateInviteCode();
        }
    }

    private static String generateInviteCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    public Team() {
    }

    // === Getters & Setters ===
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isPublished() {
        return isPublished;
    }

    public void setPublished(boolean isPublished) {
        this.isPublished = isPublished;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getCapacity() {
        return capacity;
    }

    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(int completedOrders) {
        this.completedOrders = completedOrders;
    }

    public int getCancelledOrders() {
        return cancelledOrders;
    }

    public void setCancelledOrders(int cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }
}
