package org.example.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ProductionOrderDTO {
    private String id;
    private String teamId;
    private String orderCode;
    private String title;
    private String description;
    private String customerName;
    private String productType;
    private String processType;
    private String roastLevel;
    private String packageSize;
    private Integer totalPackages;
    private Double outputTarget;
    private Double expectedYield;
    private Double expectedLoss;
    private Double inputRequired;
    private String unit;
    private LocalDate orderDate;
    private LocalDate confirmDate;
    private LocalDate productionStartDate;
    private LocalDateTime internalDeadline;
    private LocalDate customerDeliveryDate;
    private Integer safetyBufferDays;
    private String recipientName;
    private String recipientPhone;
    private String shippingNote;
    private String status;
    private Double completedQuantity;
    private Double progressPercent;
    private Double remainingQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private String contactPhoneAlt;
    private String deliveryAddress;
    private LocalDateTime preferredDeliveryFrom;
    private LocalDateTime preferredDeliveryTo;
    private String deliveryFailureAction;
    private String deliveryNote;
    private Boolean cancelRequested;
    private Boolean buyerViewed;
    private Boolean sellerViewed;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }
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
    public Double getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Double progressPercent) { this.progressPercent = progressPercent; }
    public Double getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(Double remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

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
}
