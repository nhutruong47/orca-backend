package org.example.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class InterGroupOrderDTO {

    private String id;
    private String buyerTeamId;
    private String buyerTeamName;
    private String sellerTeamId;
    private String sellerTeamName;
    private String title;
    private String description;
    private Integer quantity;
    private LocalDateTime deadline;
    private String status;
    private String linkedGoalId;
    private LocalDateTime createdAt;
    private int buyerTrustScore;
    private String cancelledBy;

    public InterGroupOrderDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBuyerTeamId() {
        return buyerTeamId;
    }

    public void setBuyerTeamId(String buyerTeamId) {
        this.buyerTeamId = buyerTeamId;
    }

    public String getBuyerTeamName() {
        return buyerTeamName;
    }

    public void setBuyerTeamName(String buyerTeamName) {
        this.buyerTeamName = buyerTeamName;
    }

    public String getSellerTeamId() {
        return sellerTeamId;
    }

    public void setSellerTeamId(String sellerTeamId) {
        this.sellerTeamId = sellerTeamId;
    }

    public String getSellerTeamName() {
        return sellerTeamName;
    }

    public void setSellerTeamName(String sellerTeamName) {
        this.sellerTeamName = sellerTeamName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLinkedGoalId() {
        return linkedGoalId;
    }

    public void setLinkedGoalId(String linkedGoalId) {
        this.linkedGoalId = linkedGoalId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getBuyerTrustScore() {
        return buyerTrustScore;
    }

    public void setBuyerTrustScore(int buyerTrustScore) {
        this.buyerTrustScore = buyerTrustScore;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }
}
