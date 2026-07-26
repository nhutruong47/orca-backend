package org.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderContractDTO {
    private UUID id;
    private UUID orderId;
    private String terms;
    private String buyerSignatureUrl;
    private String sellerSignatureUrl;
    private LocalDateTime signedAt;
    private String status;
    private String fileUrl;
    private LocalDateTime createdAt;
}
