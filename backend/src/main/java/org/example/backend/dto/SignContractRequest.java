package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignContractRequest {
    @NotBlank(message = "Chữ ký không được để trống")
    private String signatureUrl;
}
