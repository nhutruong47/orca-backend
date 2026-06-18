package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_targets")
public class DailyTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private ProductionPlan plan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private ProductionOrder order;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "target_quantity_kg")
    private Double targetQuantityKg;

    @Column(name = "target_roast_kg")
    private Double targetRoastKg;

    @Column(name = "target_qc_kg")
    private Double targetQcKg;

    @Column(name = "target_packaged_kg")
    private Double targetPackagedKg;

    @Column(name = "target_packages")
    private Integer targetPackages;

    @Column(name = "actual_roast_kg")
    private Double actualRoastKg;

    @Column(name = "actual_qc_kg")
    private Double actualQcKg;

    @Column(name = "actual_qc_fail_kg")
    private Double actualQcFailKg;

    @Column(name = "actual_packaged_kg")
    private Double actualPackagedKg;

    @Column(name = "actual_packages")
    private Integer actualPackages;

    @Column(name = "total_actual_kg")
    private Double totalActualKg;

    @Column(name = "completion_rate")
    private Double completionRate;

    @Column(name = "is_holiday")
    private Boolean isHoliday = false;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "issues", columnDefinition = "TEXT")
    private String issues;

    @Enumerated(EnumType.STRING)
    private TargetStatus status = TargetStatus.PENDING;

    @Column(name = "total_worker_hours")
    private Double totalWorkerHours;

    @Column(name = "productivity_kg_per_hour")
    private Double productivityKgPerHour;

    public enum TargetStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        PARTIAL,
        SKIPPED
    }

    @PrePersist
    protected void onCreate() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ProductionPlan getPlan() { return plan; }
    public void setPlan(ProductionPlan plan) { this.plan = plan; }
    public ProductionOrder getOrder() { return order; }
    public void setOrder(ProductionOrder order) { this.order = order; }
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
    public TargetStatus getStatus() { return status; }
    public void setStatus(TargetStatus status) { this.status = status; }
    public Double getTotalWorkerHours() { return totalWorkerHours; }
    public void setTotalWorkerHours(Double totalWorkerHours) { this.totalWorkerHours = totalWorkerHours; }
    public Double getProductivityKgPerHour() { return productivityKgPerHour; }
    public void setProductivityKgPerHour(Double productivityKgPerHour) { this.productivityKgPerHour = productivityKgPerHour; }

    public void calculateCompletionRate() {
        if (targetQuantityKg != null && targetQuantityKg > 0) {
            double actual = totalActualKg != null ? totalActualKg : 0;
            this.completionRate = Math.round((actual / targetQuantityKg) * 1000.0) / 10.0;
        }
    }

    public void calculateProductivity() {
        if (totalActualKg != null && totalWorkerHours != null && totalWorkerHours > 0) {
            this.productivityKgPerHour = Math.round((totalActualKg / totalWorkerHours) * 100.0) / 100.0;
        }
    }
}
