package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inter_group_orders")
public class InterGroupOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buyer_team_id")
    private Team buyerTeam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buyer_user_id")
    private User buyerUser;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_team_id", nullable = false)
    private Team sellerTeam;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    private LocalDateTime deadline;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, ACCEPTED, REJECTED, COMPLETED, CANCELED

    @Column(name = "linked_goal_id")
    private UUID linkedGoalId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_by")
    private String cancelledBy; // "BUYER" or "SELLER"

    // === Delivery Profile ===
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_phone_alt", length = 20)
    private String contactPhoneAlt;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "preferred_delivery_from")
    private LocalDateTime preferredDeliveryFrom;

    @Column(name = "preferred_delivery_to")
    private LocalDateTime preferredDeliveryTo;

    /** RETRY_LATER, LEAVE_AT_DOOR, RETURN_TO_SENDER, CONTACT_ALTERNATIVE */
    @Column(name = "delivery_failure_action", length = 30)
    private String deliveryFailureAction;

    @Column(name = "delivery_note", columnDefinition = "TEXT")
    private String deliveryNote;

    @Column(name = "cancel_requested")
    private Boolean cancelRequested = false;

    @Column(name = "buyer_viewed")
    private Boolean buyerViewed = true;

    @Column(name = "seller_viewed")
    private Boolean sellerViewed = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public InterGroupOrder() {
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public UUID getLinkedGoalId() {
        return linkedGoalId;
    }

    public void setLinkedGoalId(UUID linkedGoalId) {
        this.linkedGoalId = linkedGoalId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
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
}
