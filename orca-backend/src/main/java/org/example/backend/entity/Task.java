package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "task_code", unique = true, length = 40)
    private String taskCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private ProductionOrder order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id")
    private ProductionBatch batch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_id")
    private User member;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "backup_member_id")
    private User backupMember;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer priority = 1; // 1-5

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING / IN_PROGRESS / COMPLETED

    /** WAITING / ACCEPTED / REJECTED */
    @Column(name = "acceptance_status", length = 20)
    private String acceptanceStatus = "WAITING";

    @Column(name = "hourly_rate")
    private Double hourlyRate; // đơn giá mỗi giờ/đơn vị công

    private Double workload; // estimated workload (hours or units)

    @Column(name = "actual_workload")
    private Double actualWorkload; // actual workload reported by member

    @Column(name = "completion_percentage")
    private Integer completionPercentage = 0; // 0-100

    @Column(name = "production_stage", length = 80)
    private String productionStage;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "due_time")
    private LocalDateTime dueTime;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "actual_start")
    private LocalDateTime actualStart;

    @Column(name = "actual_end")
    private LocalDateTime actualEnd;

    private LocalDateTime deadline;

    @Column(name = "output_target")
    private Double outputTarget;

    @Column(name = "actual_output")
    private Double actualOutput;

    @Column(name = "defect_quantity")
    private Double defectQuantity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_by_type", length = 30)
    private String createdByType = "MANAGER";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "updated_by_type", length = 30)
    private String updatedByType = "MANAGER";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.taskCode == null || this.taskCode.isBlank()) {
            this.taskCode = "TASK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // === Getters & Setters ===
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public Goal getGoal() {
        return goal;
    }

    public void setGoal(Goal goal) {
        this.goal = goal;
    }

    public ProductionOrder getOrder() {
        return order;
    }

    public void setOrder(ProductionOrder order) {
        this.order = order;
    }

    public ProductionBatch getBatch() {
        return batch;
    }

    public void setBatch(ProductionBatch batch) {
        this.batch = batch;
    }

    public User getMember() {
        return member;
    }

    public void setMember(User member) {
        this.member = member;
    }

    public User getBackupMember() { return backupMember; }
    public void setBackupMember(User backupMember) { this.backupMember = backupMember; }

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

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getWorkload() {
        return workload;
    }

    public void setWorkload(Double workload) {
        this.workload = workload;
    }

    public Double getActualWorkload() {
        return actualWorkload;
    }

    public void setActualWorkload(Double actualWorkload) {
        this.actualWorkload = actualWorkload;
    }

    public Integer getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(Integer completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public String getProductionStage() {
        return productionStage;
    }

    public void setProductionStage(String productionStage) {
        this.productionStage = productionStage;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getDueTime() {
        return dueTime;
    }

    public void setDueTime(LocalDateTime dueTime) {
        this.dueTime = dueTime;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public LocalDateTime getActualStart() {
        return actualStart;
    }

    public void setActualStart(LocalDateTime actualStart) {
        this.actualStart = actualStart;
    }

    public LocalDateTime getActualEnd() {
        return actualEnd;
    }

    public void setActualEnd(LocalDateTime actualEnd) {
        this.actualEnd = actualEnd;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public Double getOutputTarget() {
        return outputTarget;
    }

    public void setOutputTarget(Double outputTarget) {
        this.outputTarget = outputTarget;
    }

    public Double getActualOutput() {
        return actualOutput;
    }

    public void setActualOutput(Double actualOutput) {
        this.actualOutput = actualOutput;
    }

    public Double getDefectQuantity() {
        return defectQuantity;
    }

    public void setDefectQuantity(Double defectQuantity) {
        this.defectQuantity = defectQuantity;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByType() {
        return createdByType;
    }

    public void setCreatedByType(String createdByType) {
        this.createdByType = createdByType;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedByType() {
        return updatedByType;
    }

    public void setUpdatedByType(String updatedByType) {
        this.updatedByType = updatedByType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getAcceptanceStatus() { return acceptanceStatus; }
    public void setAcceptanceStatus(String acceptanceStatus) { this.acceptanceStatus = acceptanceStatus; }

    public Double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; }
}
