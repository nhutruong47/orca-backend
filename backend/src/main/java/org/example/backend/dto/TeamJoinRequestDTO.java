package org.example.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TeamJoinRequestDTO {
    private UUID id;
    private UUID teamId;
    private String teamName;
    private UUID userId;
    private String userName;
    private String userEmail;
    private String status;
    private LocalDateTime createdAt;
}
