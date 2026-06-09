package org.example.backend.dto.ai;

import java.util.ArrayList;
import java.util.List;

public class AiReviseRequest {
    private String teamId;
    private String instruction;
    private AiPlanDraftResponse draft;
    private List<AiTeamMemberContext> members = new ArrayList<>();

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public AiPlanDraftResponse getDraft() {
        return draft;
    }

    public void setDraft(AiPlanDraftResponse draft) {
        this.draft = draft;
    }

    public List<AiTeamMemberContext> getMembers() {
        return members;
    }

    public void setMembers(List<AiTeamMemberContext> members) {
        this.members = members;
    }
}
