package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private InterGroupOrder order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buyer_team_id")
    private Team buyerTeam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buyer_user_id")
    private User buyerUser;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_team_id", nullable = false)
    private Team sellerTeam;

    /** 1–5 stars */
    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    /** ON_TIME, LATE, NOT_DELIVERED */
    @Column(name = "delivery_result", length = 20, nullable = false)
    private String deliveryResult;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Review() {
    }

    // Getters & Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public InterGroupOrder getOrder() {
        return order;
    }

    public void setOrder(InterGroupOrder order) {
        this.order = order;
    }

    public Team getBuyerTeam() {
        return buyerTeam;
    }

    public void setBuyerTeam(Team buyerTeam) {
        this.buyerTeam = buyerTeam;
    }

    public User getBuyerUser() {
        return buyerUser;
    }

    public void setBuyerUser(User buyerUser) {
        this.buyerUser = buyerUser;
    }

    public Team getSellerTeam() {
        return sellerTeam;
    }

    public void setSellerTeam(Team sellerTeam) {
        this.sellerTeam = sellerTeam;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDeliveryResult() {
        return deliveryResult;
    }

    public void setDeliveryResult(String deliveryResult) {
        this.deliveryResult = deliveryResult;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
