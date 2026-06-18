package org.example.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DailyTargetDTO {
    private String id;
    private String planId;
    private String orderId;
    private String orderTitle;
    private LocalDate targetDate;
    private Double targetQuantityKg;
    private Double targetRoastKg;
    private Double targetQcKg;
    private Double targetPackagedKg;
    private Integer targetPackages;
    private Double actualRoastKg;
    private Double actualQcKg;
    private Double actualQcFailKg;
    private Double actualPackagedKg;
    private Integer actualPackages;
    private Double totalActualKg;
    private Double completionRate;
    private Boolean isHoliday;
    private String notes;
    private String issues;
    private String status;
    private Double totalWorkerHours;
    private Double productivityKgPerHour;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getOrderTitle() { return orderTitle; }
    public void setOrderTitle(String orderTitle) { this.orderTitle = orderTitle; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public Double getTargetQuantityKg() { return targetQuantityKg; }
    public void setTargetQuantityKg(Double targetQuantityKg) { this.targetQuantityKg = targetQuantityKg; }
    public Double getTargetRoastKg() { return targetRoastKg; }
    public void setTargetRoastKg(Double targetRoastKg) { this.targetRoastKg = targetRoastKg; }
    public Double getTargetQcKg() { return targetQcKg; }
    public void setTargetQcKg(Double targetQcKg) { this.targetQcKg = targetQcKg; }
    public Double getTargetPackagedKg() { return targetPackagedKg; }
    public void setTargetPackagedKg(Double targetPackagedKg) { this.targetPackagedKg = targetPackagedKg; }
    public Integer getTargetPackages() { return targetPackages; }
    public void setTargetPackages(Integer targetPackages) { this.targetPackages = targetPackages; }
    public Double getActualRoastKg() { return actualRoastKg; }
    public void setActualRoastKg(Double actualRoastKg) { this.actualRoastKg = actualRoastKg; }
    public Double getActualQcKg() { return actualQcKg; }
    public void setActualQcKg(Double actualQcKg) { this.actualQcKg = actualQcKg; }
    public Double getActualQcFailKg() { return actualQcFailKg; }
    public void setActualQcFailKg(Double actualQcFailKg) { this.actualQcFailKg = actualQcFailKg; }
    public Double getActualPackagedKg() { return actualPackagedKg; }
    public void setActualPackagedKg(Double actualPackagedKg) { this.actualPackagedKg = actualPackagedKg; }
    public Integer getActualPackages() { return actualPackages; }
    public void setActualPackages(Integer actualPackages) { this.actualPackages = actualPackages; }
    public Double getTotalActualKg() { return totalActualKg; }
    public void setTotalActualKg(Double totalActualKg) { this.totalActualKg = totalActualKg; }
    public Double getCompletionRate() { return completionRate; }
    public void setCompletionRate(Double completionRate) { this.completionRate = completionRate; }
    public Boolean getIsHoliday() { return isHoliday; }
    public void setIsHoliday(Boolean isHoliday) { this.isHoliday = isHoliday; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getIssues() { return issues; }
    public void setIssues(String issues) { this.issues = issues; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getTotalWorkerHours() { return totalWorkerHours; }
    public void setTotalWorkerHours(Double totalWorkerHours) { this.totalWorkerHours = totalWorkerHours; }
    public Double getProductivityKgPerHour() { return productivityKgPerHour; }
    public void setProductivityKgPerHour(Double productivityKgPerHour) { this.productivityKgPerHour = productivityKgPerHour; }
}
