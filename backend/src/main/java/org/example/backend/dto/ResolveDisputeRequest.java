package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResolveDisputeRequest {
    @NotBlank(message = "Ghi chú giải quyết không được để trống")
    private String resolutionNote;
}
