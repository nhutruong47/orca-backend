package org.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "goals")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner; // Group Owner who created this goal

    @Column(nullable = false, length = 500)
    private String title;

    @org.hibernate.annotations.Nationalized
    @Column(name = "output_target", columnDefinition = "NVARCHAR(MAX)")
    private String outputTarget; // e.g. "3 tấn cà phê"

    @org.hibernate.annotations.Nationalized
    @Column(name = "raw_instruction", columnDefinition = "NVARCHAR(MAX)")
    private String rawInstruction;

    @org.hibernate.annotations.Nationalized
    @Column(name = "ai_parsed_data", columnDefinition = "NVARCHAR(MAX)")
    private String aiParsedData;

    private Integer priority = 1;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING / PLANNING / PUBLISHED / DONE

    private LocalDateTime deadline;

    @Column(name = "total_tasks")
    private Integer totalTasks = 0;

    @Column(name = "completed_tasks")
    private Integer completedTasks = 0;

    @org.hibernate.annotations.Nationalized
    @Column(name = "chat_log", columnDefinition = "NVARCHAR(MAX)")
    private String chatLog;

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

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
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

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getChatLog() {
        return chatLog;
    }

    public void setChatLog(String chatLog) {
        this.chatLog = chatLog;
    }
}
