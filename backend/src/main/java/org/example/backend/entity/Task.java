package org.example.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "goal_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Goal goal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User member;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "backup_member_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User backupMember;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Min(1)
    @Max(5)
    private Integer priority = 1; // 1-5

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING / IN_PROGRESS / COMPLETED

    /** WAITING / ACCEPTED / REJECTED */
    @Column(name = "acceptance_status", length = 20)
    private String acceptanceStatus = "WAITING";

    @Column(name = "hourly_rate")
    private Double hourlyRate; // đơn giá mỗi giờ/đơn vị công

    @Min(0)
    private Double workload; // estimated workload (hours or units)

    @Min(0)
    @Column(name = "actual_workload")
    private Double actualWorkload; // actual workload reported by member

    @Min(0)
    @Max(100)
    @Column(name = "completion_percentage")
    private Integer completionPercentage = 0; // 0-100

    @Min(0)
    @Column(name = "output_target")
    private Double outputTarget;

    @Min(0)
    @Column(name = "actual_output")
    private Double actualOutput;

    private LocalDateTime deadline;

    @Column(name = "production_stage")
    private String productionStage;

    @Column(name = "due_time")
    private LocalDateTime dueTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // === Getters & Setters ===
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Goal getGoal() {
        return goal;
    }

    public void setGoal(Goal goal) {
        this.goal = goal;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getAcceptanceStatus() { return acceptanceStatus; }
    public void setAcceptanceStatus(String acceptanceStatus) { this.acceptanceStatus = acceptanceStatus; }

    public Double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; }

    public String getProductionStage() { return productionStage; }
    public void setProductionStage(String productionStage) { this.productionStage = productionStage; }

    public LocalDateTime getDueTime() { return dueTime; }
    public void setDueTime(LocalDateTime dueTime) { this.dueTime = dueTime; }
}
