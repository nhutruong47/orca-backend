package org.example.backend.dto.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiPlanRequest {
    private String teamId;
    private String intent;
    private Map<String, Object> fields = new HashMap<>();
    private List<AiTeamMemberContext> members = new ArrayList<>();

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }

    public List<AiTeamMemberContext> getMembers() {
        return members;
    }

    public void setMembers(List<AiTeamMemberContext> members) {
        this.members = members;
    }
}
