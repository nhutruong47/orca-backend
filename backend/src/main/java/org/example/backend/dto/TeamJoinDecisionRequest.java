package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TeamJoinDecisionRequest {
    @NotBlank(message = "Decision must be APPROVED or REJECTED")
    private String decision;
}
