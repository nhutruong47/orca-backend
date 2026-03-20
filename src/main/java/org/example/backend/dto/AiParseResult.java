package org.example.backend.dto;

import java.util.List;
import java.util.Map;

public class AiParseResult {
    private String title;
    private String description;
    private String quantity;
    private Double quantityNumber;
    private String unit;
    private String deadline;
    private String priority;
    private Boolean needsClarification;
    private String source; // "gemini" or "regex"
    private List<Map<String, Object>> tasks;

    // Additional fields from older versions (optional, but good for backward compatibility)
    private String phase;
    private String mainGoal;
    private String contingency;

    public AiParseResult() {
    }

    public String getTitle() {
        return title != null ? title : mainGoal;
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

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public Double getQuantityNumber() {
        return quantityNumber;
    }

    public void setQuantityNumber(Double quantityNumber) {
        this.quantityNumber = quantityNumber;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Boolean getNeedsClarification() {
        return needsClarification;
    }

    public void setNeedsClarification(Boolean needsClarification) {
        this.needsClarification = needsClarification;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<Map<String, Object>> getTasks() {
        return tasks;
    }

    public void setTasks(List<Map<String, Object>> tasks) {
        this.tasks = tasks;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getMainGoal() {
        return mainGoal;
    }

    public void setMainGoal(String mainGoal) {
        this.mainGoal = mainGoal;
    }

    public String getContingency() {
        return contingency;
    }

    public void setContingency(String contingency) {
        this.contingency = contingency;
    }
}
