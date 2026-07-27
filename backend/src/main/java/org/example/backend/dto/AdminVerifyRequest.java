package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminVerifyRequest {
    @NotBlank(message = "Status must be APPROVED or REJECTED")
    private String status;
    private String adminNote;
}
