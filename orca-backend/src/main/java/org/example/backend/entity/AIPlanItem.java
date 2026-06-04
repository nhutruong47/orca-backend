package org.example.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "ai_plan_items")
public class AIPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ai_plan_id", nullable = false)
    private AIPlan aiPlan;

    @Column(name = "item_type", nullable = false)
    private String itemType;

    @Column(nullable = false)
    private String status = "DRAFT";

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String rationale;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public AIPlan getAiPlan() { return aiPlan; }
    public void setAiPlan(AIPlan aiPlan) { this.aiPlan = aiPlan; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
}
