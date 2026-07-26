package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.OrderContractDTO;
import org.example.backend.dto.SignContractRequest;
import org.example.backend.entity.User;
import org.example.backend.service.OrderContractService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class OrderContractController {

    private final OrderContractService contractService;

    @PostMapping("/order/{orderId}")
    public ResponseEntity<OrderContractDTO> createContract(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(contractService.createContract(orderId, currentUser));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<OrderContractDTO> getContractByOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(contractService.getContractByOrderId(orderId, currentUser));
    }

    @PostMapping("/{contractId}/sign")
    public ResponseEntity<OrderContractDTO> signContract(
            @PathVariable UUID contractId,
            @Valid @RequestBody SignContractRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(contractService.signContract(contractId, request.getSignatureUrl(), currentUser));
    }
}
