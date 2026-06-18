package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "production_plans")
public class ProductionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private ProductionOrder order;

    @Column(name = "plan_code", unique = true)
    private String planCode;

    @Column(name = "total_days")
    private Integer totalDays;

    @Column(name = "total_working_days")
    private Integer totalWorkingDays;

    @Column(name = "daily_target_kg")
    private Double dailyTargetKg;

    @Column(name = "total_input_kg")
    private Double totalInputKg;

    @Column(name = "total_roast_kg")
    private Double totalRoastKg;

    @Column(name = "total_qc_kg")
    private Double totalQcKg;

    @Column(name = "total_packaged_kg")
    private Double totalPackagedKg;

    @Column(name = "total_packages")
    private Integer totalPackages;

    @Column(name = "ai_recommendations", columnDefinition = "TEXT")
    private String aiRecommendations;

    @Column(name = "risk_factors", columnDefinition = "TEXT")
    private String riskFactors;

    @Enumerated(EnumType.STRING)
    private PlanStatus status = PlanStatus.DRAFT;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DailyTarget> dailyTargets;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    public enum PlanStatus {
        DRAFT,
        PENDING,
        APPROVED,
        IN_PROGRESS,
        COMPLETED
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.planCode == null || this.planCode.isBlank()) {
            this.planCode = "PLAN-" + System.currentTimeMillis();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ProductionOrder getOrder() { return order; }
    public void setOrder(ProductionOrder order) { this.order = order; }
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
    public PlanStatus getStatus() { return status; }
    public void setStatus(PlanStatus status) { this.status = status; }
    public List<DailyTarget> getDailyTargets() { return dailyTargets; }
    public void setDailyTargets(List<DailyTarget> dailyTargets) { this.dailyTargets = dailyTargets; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public UUID getApprovedBy() { return approvedBy; }
    public void setApprovedBy(UUID approvedBy) { this.approvedBy = approvedBy; }
}
