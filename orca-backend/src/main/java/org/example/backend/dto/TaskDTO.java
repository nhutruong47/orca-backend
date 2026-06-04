package org.example.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class TaskDTO {
    private String id;
    private String taskCode;
    private String title;
    private String description;
    private Integer priority;
    private String status;
    private String acceptanceStatus;
    private Double hourlyRate;
    private Double workload;
    private Double actualWorkload;
    private Integer completionPercentage;
    private String productionStage;
    private LocalDateTime startTime;
    private LocalDateTime dueTime;
    private Integer estimatedDurationMinutes;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private Double outputTarget;
    private Double actualOutput;
    private Double defectQuantity;
    private LocalDateTime deadline;
    private String orderId;
    private String orderCode;
    private String batchId;
    private String batchCode;
    private String goalId;
    private String goalTitle;
    private String memberId;
    private String memberName;
    private String backupMemberId;
    private String backupMemberName;
    private String supervisorId;
    private String supervisorName;
    private List<String> dependencyTaskCodes;
    private List<String> dependencyTaskTitles;
    private String createdById;
    private String createdByName;
    private String createdByType;
    private String updatedById;
    private String updatedByName;
    private String updatedByType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // === Getters & Setters ===
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
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

    public String getProductionStage() { return productionStage; }
    public void setProductionStage(String productionStage) { this.productionStage = productionStage; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getDueTime() { return dueTime; }
    public void setDueTime(LocalDateTime dueTime) { this.dueTime = dueTime; }

    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

    public LocalDateTime getActualStart() { return actualStart; }
    public void setActualStart(LocalDateTime actualStart) { this.actualStart = actualStart; }

    public LocalDateTime getActualEnd() { return actualEnd; }
    public void setActualEnd(LocalDateTime actualEnd) { this.actualEnd = actualEnd; }

    public Double getOutputTarget() { return outputTarget; }
    public void setOutputTarget(Double outputTarget) { this.outputTarget = outputTarget; }

    public Double getActualOutput() { return actualOutput; }
    public void setActualOutput(Double actualOutput) { this.actualOutput = actualOutput; }

    public Double getDefectQuantity() { return defectQuantity; }
    public void setDefectQuantity(Double defectQuantity) { this.defectQuantity = defectQuantity; }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }

    public String getGoalId() {
        return goalId;
    }

    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }

    public String getGoalTitle() {
        return goalTitle;
    }

    public void setGoalTitle(String goalTitle) {
        this.goalTitle = goalTitle;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getBackupMemberId() { return backupMemberId; }
    public void setBackupMemberId(String backupMemberId) { this.backupMemberId = backupMemberId; }

    public String getBackupMemberName() { return backupMemberName; }
    public void setBackupMemberName(String backupMemberName) { this.backupMemberName = backupMemberName; }

    public String getSupervisorId() { return supervisorId; }
    public void setSupervisorId(String supervisorId) { this.supervisorId = supervisorId; }

    public String getSupervisorName() { return supervisorName; }
    public void setSupervisorName(String supervisorName) { this.supervisorName = supervisorName; }

    public List<String> getDependencyTaskCodes() { return dependencyTaskCodes; }
    public void setDependencyTaskCodes(List<String> dependencyTaskCodes) { this.dependencyTaskCodes = dependencyTaskCodes; }

    public List<String> getDependencyTaskTitles() { return dependencyTaskTitles; }
    public void setDependencyTaskTitles(List<String> dependencyTaskTitles) { this.dependencyTaskTitles = dependencyTaskTitles; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedById() { return createdById; }
    public void setCreatedById(String createdById) { this.createdById = createdById; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public String getCreatedByType() { return createdByType; }
    public void setCreatedByType(String createdByType) { this.createdByType = createdByType; }

    public String getUpdatedById() { return updatedById; }
    public void setUpdatedById(String updatedById) { this.updatedById = updatedById; }

    public String getUpdatedByName() { return updatedByName; }
    public void setUpdatedByName(String updatedByName) { this.updatedByName = updatedByName; }

    public String getUpdatedByType() { return updatedByType; }
    public void setUpdatedByType(String updatedByType) { this.updatedByType = updatedByType; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getAcceptanceStatus() { return acceptanceStatus; }
    public void setAcceptanceStatus(String acceptanceStatus) { this.acceptanceStatus = acceptanceStatus; }

    public Double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; }
}
