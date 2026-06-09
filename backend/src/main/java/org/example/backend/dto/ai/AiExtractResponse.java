package org.example.backend.dto.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiExtractResponse {
    private String intent;
    private Double confidence;
    private Map<String, Object> fields = new HashMap<>();
    private List<String> missingFields = new ArrayList<>();
    private String clarifyingQuestion;

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }

    public String getClarifyingQuestion() {
        return clarifyingQuestion;
    }

    public void setClarifyingQuestion(String clarifyingQuestion) {
        this.clarifyingQuestion = clarifyingQuestion;
    }
}
