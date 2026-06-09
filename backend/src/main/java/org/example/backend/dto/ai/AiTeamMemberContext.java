package org.example.backend.dto.ai;

import java.util.ArrayList;
import java.util.List;

public class AiTeamMemberContext {
    private String userId;
    private String username;
    private String fullName;
    private List<String> jobLabels = new ArrayList<>();

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public List<String> getJobLabels() {
        return jobLabels;
    }

    public void setJobLabels(List<String> jobLabels) {
        this.jobLabels = jobLabels;
    }
}
