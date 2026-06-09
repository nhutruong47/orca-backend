package org.example.backend.dto.ai;

public class AiTaskDraft {
    private String title;
    private String description;
    private Integer priority;
    private Double workload;
    private String suggestedAssigneeId;
    private String suggestedAssigneeName;
    private String suggestedReason;

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

    public Double getWorkload() {
        return workload;
    }

    public void setWorkload(Double workload) {
        this.workload = workload;
    }

    public String getSuggestedAssigneeId() {
        return suggestedAssigneeId;
    }

    public void setSuggestedAssigneeId(String suggestedAssigneeId) {
        this.suggestedAssigneeId = suggestedAssigneeId;
    }

    public String getSuggestedAssigneeName() {
        return suggestedAssigneeName;
    }

    public void setSuggestedAssigneeName(String suggestedAssigneeName) {
        this.suggestedAssigneeName = suggestedAssigneeName;
    }

    public String getSuggestedReason() {
        return suggestedReason;
    }

    public void setSuggestedReason(String suggestedReason) {
        this.suggestedReason = suggestedReason;
    }
}
