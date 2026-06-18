package org.example.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ProductionPlanDTO {
    private String id;
    private String orderId;
    private String orderTitle;
    private String planCode;
    private Integer totalDays;
    private Integer totalWorkingDays;
    private Double dailyTargetKg;
    private Double totalInputKg;
    private Double totalRoastKg;
    private Double totalQcKg;
    private Double totalPackagedKg;
    private Integer totalPackages;
    private String aiRecommendations;
    private String riskFactors;
    private String status;
    private List<DailyTargetDTO> dailyTargets;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private String approvedBy;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getOrderTitle() { return orderTitle; }
    public void setOrderTitle(String orderTitle) { this.orderTitle = orderTitle; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public Integer getTotalDays() { return totalDays; }
    public void setTotalDays(Integer totalDays) { this.totalDays = totalDays; }
    public Integer getTotalWorkingDays() { return totalWorkingDays; }
    public void setTotalWorkingDays(Integer totalWorkingDays) { this.totalWorkingDays = totalWorkingDays; }
    public Double getDailyTargetKg() { return dailyTargetKg; }
    public void setDailyTargetKg(Double dailyTargetKg) { this.dailyTargetKg = dailyTargetKg; }
    public Double getTotalInputKg() { return totalInputKg; }
    public void setTotalInputKg(Double totalInputKg) { this.totalInputKg = totalInputKg; }
    public Double getTotalRoastKg() { return totalRoastKg; }
    public void setTotalRoastKg(Double totalRoastKg) { this.totalRoastKg = totalRoastKg; }
    public Double getTotalQcKg() { return totalQcKg; }
    public void setTotalQcKg(Double totalQcKg) { this.totalQcKg = totalQcKg; }
    public Double getTotalPackagedKg() { return totalPackagedKg; }
    public void setTotalPackagedKg(Double totalPackagedKg) { this.totalPackagedKg = totalPackagedKg; }
    public Integer getTotalPackages() { return totalPackages; }
    public void setTotalPackages(Integer totalPackages) { this.totalPackages = totalPackages; }
    public String getAiRecommendations() { return aiRecommendations; }
    public void setAiRecommendations(String aiRecommendations) { this.aiRecommendations = aiRecommendations; }
    public String getRiskFactors() { return riskFactors; }
    public void setRiskFactors(String riskFactors) { this.riskFactors = riskFactors; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<DailyTargetDTO> getDailyTargets() { return dailyTargets; }
    public void setDailyTargets(List<DailyTargetDTO> dailyTargets) { this.dailyTargets = dailyTargets; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
}
