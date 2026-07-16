package org.example.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persisted AI plan draft.
 *
 * An AI plan is the structured output of the RAG pipeline that a team
 * owner can edit and ultimately promote into a real Goal (and the
 * downstream Tasks). Keeping it as a separate entity means AI drafts
 * don't pollute the production Goal table — the user has to explicitly
 * accept the plan to convert it.
 *
 * Lifecycle:
 *   DRAFT -> REVISED -> APPROVED -> (optionally) PROMOTED
 *                        \-> REJECTED
 *                        \-> EXPIRED
 */
@Entity
@Table(name = "ai_plans", indexes = {
        @Index(name = "idx_ai_plans_team", columnList = "team_id"),
        @Index(name = "idx_ai_plans_owner", columnList = "owner_id"),
        @Index(name = "idx_ai_plans_status", columnList = "status")
})
public class AiPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "source_query", columnDefinition = "TEXT")
    private String sourceQuery;

    @Column(name = "intent", length = 50)
    private String intent;

    @Column(name = "goal_title", length = 500)
    private String goalTitle;

    @Column(name = "output_target", columnDefinition = "TEXT")
    private String outputTarget;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "priority")
    private Integer priority;

    /** JSON-serialized list of TaskDraft items. */
    @Column(name = "tasks_json", columnDefinition = "TEXT")
    private String tasksJson;

    /** Full conversation that produced this plan (for replay). */
    @Column(name = "conversation_id", length = 100)
    private String conversationId;

    /** JSON-serialized citations / referenced knowledge. */
    @Column(name = "referenced_knowledge_json", columnDefinition = "TEXT")
    private String referencedKnowledgeJson;

    /** JSON-serialized suggested actions. */
    @Column(name = "suggested_actions_json", columnDefinition = "TEXT")
    private String suggestedActionsJson;

    @Column(name = "reasoning_summary", columnDefinition = "TEXT")
    private String reasoningSummary;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** If non-null, points to the Goal that was created from this plan. */
    @Column(name = "promoted_goal_id")
    private UUID promotedGoalId;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.status == null) {
            this.status = "DRAFT";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---- Getters / Setters ----

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public String getSourceQuery() { return sourceQuery; }
    public void setSourceQuery(String sourceQuery) { this.sourceQuery = sourceQuery; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getGoalTitle() { return goalTitle; }
    public void setGoalTitle(String goalTitle) { this.goalTitle = goalTitle; }

    public String getOutputTarget() { return outputTarget; }
    public void setOutputTarget(String outputTarget) { this.outputTarget = outputTarget; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getTasksJson() { return tasksJson; }
    public void setTasksJson(String tasksJson) { this.tasksJson = tasksJson; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getReferencedKnowledgeJson() { return referencedKnowledgeJson; }
    public void setReferencedKnowledgeJson(String referencedKnowledgeJson) { this.referencedKnowledgeJson = referencedKnowledgeJson; }

    public String getSuggestedActionsJson() { return suggestedActionsJson; }
    public void setSuggestedActionsJson(String suggestedActionsJson) { this.suggestedActionsJson = suggestedActionsJson; }

    public String getReasoningSummary() { return reasoningSummary; }
    public void setReasoningSummary(String reasoningSummary) { this.reasoningSummary = reasoningSummary; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public UUID getPromotedGoalId() { return promotedGoalId; }
    public void setPromotedGoalId(UUID promotedGoalId) { this.promotedGoalId = promotedGoalId; }
}