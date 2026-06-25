package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "production_orders")
public class ProductionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "order_code", unique = true, length = 40)
    private String orderCode;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "customer_name")
    private String customerName;

    // === THONG TIN SAN PHAM ===
    @Column(name = "product_type", length = 100)
    private String productType;

    @Column(name = "process_type", length = 100)
    private String processType;

    @Column(name = "roast_level", length = 50)
    private String roastLevel;

    @Column(name = "package_size", length = 100)
    private String packageSize;

    @Column(name = "total_packages")
    private Integer totalPackages;

    // === YIELD & INPUT ===
    @Column(name = "output_target")
    private Double outputTarget;

    @Column(name = "expected_yield")
    private Double expectedYield;

    @Column(name = "expected_loss")
    private Double expectedLoss;

    @Column(name = "input_required")
    private Double inputRequired;

    private String unit;

    // === CAC NGAY QUAN TRONG ===
    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "confirm_date")
    private LocalDate confirmDate;

    @Column(name = "production_start_date")
    private LocalDate productionStartDate;

    @Column(name = "internal_deadline")
    private LocalDateTime internalDeadline;

    @Column(name = "customer_delivery_date")
    private LocalDate customerDeliveryDate;

    @Column(name = "safety_buffer_days")
    private Integer safetyBufferDays;

    // === GIAO HANG ===
    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "recipient_phone")
    private String recipientPhone;

    @Column(name = "shipping_note", columnDefinition = "TEXT")
    private String shippingNote;

    // === TRANG THAI & SẢN LƯỢNG ===
    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "completed_quantity")
    private Double completedQuantity;

    @Column(name = "contact_phone_alt", length = 20)
    private String contactPhoneAlt;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "preferred_delivery_from")
    private LocalDateTime preferredDeliveryFrom;

    @Column(name = "preferred_delivery_to")
    private LocalDateTime preferredDeliveryTo;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.orderCode == null || this.orderCode.isBlank()) {
            this.orderCode = "PO-" + System.currentTimeMillis();
        }
        if (this.safetyBufferDays == null) {
            this.safetyBufferDays = 2;
        }
        calculateInternalDeadline();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateInternalDeadline();
    }

    private void calculateInternalDeadline() {
        if (this.customerDeliveryDate != null && this.safetyBufferDays != null) {
            this.internalDeadline = this.customerDeliveryDate
                    .atStartOfDay()
                    .minusDays(this.safetyBufferDays);
        }
    }

    public void calculateInputRequired() {
        if (this.outputTarget != null && this.expectedYield != null && this.expectedYield > 0) {
            this.inputRequired = Math.ceil((this.outputTarget / this.expectedYield) * 100.0) / 100.0;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public String getProcessType() { return processType; }
    public void setProcessType(String processType) { this.processType = processType; }
    public String getRoastLevel() { return roastLevel; }
    public void setRoastLevel(String roastLevel) { this.roastLevel = roastLevel; }
    public String getPackageSize() { return packageSize; }
    public void setPackageSize(String packageSize) { this.packageSize = packageSize; }
    public Integer getTotalPackages() { return totalPackages; }
    public void setTotalPackages(Integer totalPackages) { this.totalPackages = totalPackages; }
    public Double getOutputTarget() { return outputTarget; }
    public void setOutputTarget(Double outputTarget) { this.outputTarget = outputTarget; }
    public Double getExpectedYield() { return expectedYield; }
    public void setExpectedYield(Double expectedYield) { this.expectedYield = expectedYield; }
    public Double getExpectedLoss() { return expectedLoss; }
    public void setExpectedLoss(Double expectedLoss) { this.expectedLoss = expectedLoss; }
    public Double getInputRequired() { return inputRequired; }
    public void setInputRequired(Double inputRequired) { this.inputRequired = inputRequired; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public LocalDate getConfirmDate() { return confirmDate; }
    public void setConfirmDate(LocalDate confirmDate) { this.confirmDate = confirmDate; }
    public LocalDate getProductionStartDate() { return productionStartDate; }
    public void setProductionStartDate(LocalDate productionStartDate) { this.productionStartDate = productionStartDate; }
    public LocalDateTime getInternalDeadline() { return internalDeadline; }
    public void setInternalDeadline(LocalDateTime internalDeadline) { this.internalDeadline = internalDeadline; }
    public LocalDate getCustomerDeliveryDate() { return customerDeliveryDate; }
    public void setCustomerDeliveryDate(LocalDate customerDeliveryDate) { this.customerDeliveryDate = customerDeliveryDate; }
    public Integer getSafetyBufferDays() { return safetyBufferDays; }
    public void setSafetyBufferDays(Integer safetyBufferDays) { this.safetyBufferDays = safetyBufferDays; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }
    public String getShippingNote() { return shippingNote; }
    public void setShippingNote(String shippingNote) { this.shippingNote = shippingNote; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getCompletedQuantity() { return completedQuantity; }
    public void setCompletedQuantity(Double completedQuantity) { this.completedQuantity = completedQuantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public String getContactPhoneAlt() { return contactPhoneAlt; }
    public void setContactPhoneAlt(String contactPhoneAlt) { this.contactPhoneAlt = contactPhoneAlt; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public LocalDateTime getPreferredDeliveryFrom() { return preferredDeliveryFrom; }
    public void setPreferredDeliveryFrom(LocalDateTime preferredDeliveryFrom) { this.preferredDeliveryFrom = preferredDeliveryFrom; }
    public LocalDateTime getPreferredDeliveryTo() { return preferredDeliveryTo; }
    public void setPreferredDeliveryTo(LocalDateTime preferredDeliveryTo) { this.preferredDeliveryTo = preferredDeliveryTo; }
    public String getDeliveryFailureAction() { return deliveryFailureAction; }
    public void setDeliveryFailureAction(String deliveryFailureAction) { this.deliveryFailureAction = deliveryFailureAction; }
    public String getDeliveryNote() { return deliveryNote; }
    public void setDeliveryNote(String deliveryNote) { this.deliveryNote = deliveryNote; }
    public Boolean getCancelRequested() { return cancelRequested; }
    public void setCancelRequested(Boolean cancelRequested) { this.cancelRequested = cancelRequested; }
    public Boolean getBuyerViewed() { return buyerViewed; }
    public void setBuyerViewed(Boolean buyerViewed) { this.buyerViewed = buyerViewed; }
    public Boolean getSellerViewed() { return sellerViewed; }
    public void setSellerViewed(Boolean sellerViewed) { this.sellerViewed = sellerViewed; }

    public double getProgressPercent() {
        if (outputTarget == null || outputTarget == 0) return 0;
        double completed = completedQuantity != null ? completedQuantity : 0;
        return Math.min(100, Math.round((completed / outputTarget) * 1000.0) / 10.0);
    }

    public double getRemainingQuantity() {
        double completed = completedQuantity != null ? completedQuantity : 0;
        return Math.max(0, (outputTarget != null ? outputTarget : 0) - completed);
    }
}
