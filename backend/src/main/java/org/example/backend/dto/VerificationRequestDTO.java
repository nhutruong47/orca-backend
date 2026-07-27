package org.example.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class VerificationRequestDTO {
    private UUID id;
    private UUID teamId;
    private String teamName;
    private String requestedBy;
    private String documentUrl;
    private String status;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
