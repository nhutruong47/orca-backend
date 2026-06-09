package org.example.backend.dto;

import java.time.LocalDateTime;

public class GoalDTO {
    private String id;
    private String title;
    private String outputTarget;
    private String rawInstruction;
    private String aiParsedData;
    private Integer priority;
    private String status;
    private String deadline;
    private Integer totalTasks;
    private Integer completedTasks;
    private String teamId;
    private String teamName;
    private String ownerId;
    private String ownerName;
    private LocalDateTime createdAt;
    private Boolean useAi;
    private String chatLog;
    private java.util.List<java.util.Map<String, Object>> tasks;

    // === Getters & Setters ===
    public java.util.List<java.util.Map<String, Object>> getTasks() {
        return tasks;
    }

    public void setTasks(java.util.List<java.util.Map<String, Object>> tasks) {
        this.tasks = tasks;
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOutputTarget() {
        return outputTarget;
    }

    public void setOutputTarget(String outputTarget) {
        this.outputTarget = outputTarget;
    }

    public String getRawInstruction() {
        return rawInstruction;
    }

    public void setRawInstruction(String rawInstruction) {
        this.rawInstruction = rawInstruction;
    }

    public String getAiParsedData() {
        return aiParsedData;
    }

    public void setAiParsedData(String aiParsedData) {
        this.aiParsedData = aiParsedData;
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

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public Integer getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(Integer totalTasks) {
        this.totalTasks = totalTasks;
    }

    public Integer getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(Integer completedTasks) {
        this.completedTasks = completedTasks;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getUseAi() {
        return useAi;
    }

    public void setUseAi(Boolean useAi) {
        this.useAi = useAi;
    }

    public String getChatLog() {
        return chatLog;
    }

    public void setChatLog(String chatLog) {
        this.chatLog = chatLog;
    }
}
