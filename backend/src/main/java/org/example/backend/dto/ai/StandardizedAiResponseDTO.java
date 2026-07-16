package org.example.backend.dto.ai;

import java.util.List;
import java.util.Map;

/**
 * DTO matching the AI service /api/rag/query response shape.
 *
 * This is intentionally permissive (Map for cited fields) so that any
 * future schema change in the AI service does not require a backend
 * restart.
 */
public class StandardizedAiResponseDTO {

    public String answer;
    public String reasoningSummary;
    public List<Map<String, Object>> referencedKnowledge;
    public Map<String, Object> confidence;
    public List<Map<String, Object>> suggestedActions;
    public Map<String, Object> metadata;

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getReasoningSummary() { return reasoningSummary; }
    public void setReasoningSummary(String reasoningSummary) { this.reasoningSummary = reasoningSummary; }

    public List<Map<String, Object>> getReferencedKnowledge() { return referencedKnowledge; }
    public void setReferencedKnowledge(List<Map<String, Object>> referencedKnowledge) {
        this.referencedKnowledge = referencedKnowledge;
    }

    public Map<String, Object> getConfidence() { return confidence; }
    public void setConfidence(Map<String, Object> confidence) { this.confidence = confidence; }

    public List<Map<String, Object>> getSuggestedActions() { return suggestedActions; }
    public void setSuggestedActions(List<Map<String, Object>> suggestedActions) {
        this.suggestedActions = suggestedActions;
    }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}