package org.example.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class InterGroupOrderDTO {

    private String id;
    private String buyerTeamId;
    private String buyerTeamName;
    private String buyerUserId;
    private String buyerUserName;
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

    // === Delivery Profile ===
    private String contactPhone;
    private String contactPhoneAlt;
    private String deliveryAddress;
    private LocalDateTime preferredDeliveryFrom;
    private LocalDateTime preferredDeliveryTo;
    private String deliveryFailureAction;
    private String deliveryNote;
    private Boolean cancelRequested;
    private Boolean buyerViewed;
    private Boolean sellerViewed;

    // === Delivery Confirmation ===
    private Boolean deliveryConfirmed;
    private String deliveryStatus; // ON_TIME, LATE, NOT_DELIVERED
    private LocalDateTime deliveryConfirmedAt;

    // === Marketplace RFQ Fields ===
    private String materialSource; // CUSTOMER_PROVIDED, FACTORY_PROVIDED, COMBINED
    private String services; // comma-separated: ROASTING, PACKAGING, QC, etc.
    private String productType; // ARABICA, ROBUSTA, CULI, BLEND
    private Double quotedPrice;
    private String quotedNote;
    private LocalDateTime quotedAt;
    private String unit;

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

    public String getBuyerUserId() {
        return buyerUserId;
    }

    public void setBuyerUserId(String buyerUserId) {
        this.buyerUserId = buyerUserId;
    }

    public String getBuyerUserName() {
        return buyerUserName;
    }

    public void setBuyerUserName(String buyerUserName) {
        this.buyerUserName = buyerUserName;
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

    // === Delivery Profile Getters & Setters ===

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactPhoneAlt() {
        return contactPhoneAlt;
    }

    public void setContactPhoneAlt(String contactPhoneAlt) {
        this.contactPhoneAlt = contactPhoneAlt;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public LocalDateTime getPreferredDeliveryFrom() {
        return preferredDeliveryFrom;
    }

    public void setPreferredDeliveryFrom(LocalDateTime preferredDeliveryFrom) {
        this.preferredDeliveryFrom = preferredDeliveryFrom;
    }

    public LocalDateTime getPreferredDeliveryTo() {
        return preferredDeliveryTo;
    }

    public void setPreferredDeliveryTo(LocalDateTime preferredDeliveryTo) {
        this.preferredDeliveryTo = preferredDeliveryTo;
    }

    public String getDeliveryFailureAction() {
        return deliveryFailureAction;
    }

    public void setDeliveryFailureAction(String deliveryFailureAction) {
        this.deliveryFailureAction = deliveryFailureAction;
    }

    public String getDeliveryNote() {
        return deliveryNote;
    }

    public void setDeliveryNote(String deliveryNote) {
        this.deliveryNote = deliveryNote;
    }

    public Boolean getCancelRequested() {
        return cancelRequested;
    }

    public void setCancelRequested(Boolean cancelRequested) {
        this.cancelRequested = cancelRequested;
    }

    public Boolean getBuyerViewed() {
        return buyerViewed;
    }

    public void setBuyerViewed(Boolean buyerViewed) {
        this.buyerViewed = buyerViewed;
    }

    public Boolean getSellerViewed() {
        return sellerViewed;
    }

    public void setSellerViewed(Boolean sellerViewed) {
        this.sellerViewed = sellerViewed;
    }

    public Boolean getDeliveryConfirmed() {
        return deliveryConfirmed;
    }

    public void setDeliveryConfirmed(Boolean deliveryConfirmed) {
        this.deliveryConfirmed = deliveryConfirmed;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public LocalDateTime getDeliveryConfirmedAt() {
        return deliveryConfirmedAt;
    }

    public void setDeliveryConfirmedAt(LocalDateTime deliveryConfirmedAt) {
        this.deliveryConfirmedAt = deliveryConfirmedAt;
    }

    // === Marketplace RFQ Getters & Setters ===

    public String getMaterialSource() { return materialSource; }
    public void setMaterialSource(String materialSource) { this.materialSource = materialSource; }

    public String getServices() { return services; }
    public void setServices(String services) { this.services = services; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public Double getQuotedPrice() { return quotedPrice; }
    public void setQuotedPrice(Double quotedPrice) { this.quotedPrice = quotedPrice; }

    public String getQuotedNote() { return quotedNote; }
    public void setQuotedNote(String quotedNote) { this.quotedNote = quotedNote; }

    public LocalDateTime getQuotedAt() { return quotedAt; }
    public void setQuotedAt(LocalDateTime quotedAt) { this.quotedAt = quotedAt; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
