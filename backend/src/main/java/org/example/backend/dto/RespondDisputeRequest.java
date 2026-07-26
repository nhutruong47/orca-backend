package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class RespondDisputeRequest {
    @NotBlank(message = "Nội dung phản hồi không được để trống")
    private String note;
    
    private List<String> evidenceUrls;
}
