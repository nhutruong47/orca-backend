package org.example.backend.dto;

public class AiParseResult {
    private String phase;
    private String mainGoal;
    private String contingency;
    private Boolean needsClarification;
    private String description; // message for needsClarification
    private String source; // "gemini" or "regex"
    private java.util.List<java.util.Map<String, Object>> tasks;

    public AiParseResult() {
    }

    public AiParseResult(String phase, String mainGoal, String contingency, Boolean needsClarification, String description, String source) {
        this.phase = phase;
        this.mainGoal = mainGoal;
        this.contingency = contingency;
        this.needsClarification = needsClarification;
        this.description = description;
        this.source = source;
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

    public Boolean getNeedsClarification() {
        return needsClarification;
    }

    public void setNeedsClarification(Boolean needsClarification) {
        this.needsClarification = needsClarification;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public java.util.List<java.util.Map<String, Object>> getTasks() {
        return tasks;
    }

    public void setTasks(java.util.List<java.util.Map<String, Object>> tasks) {
        this.tasks = tasks;
    }
}
