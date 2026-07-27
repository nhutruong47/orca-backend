package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.UUID;

@Data
public class SubmitVerificationRequest {
    private UUID teamId;
    
    @NotBlank(message = "Document URL cannot be empty")
    private String documentUrl;
}
