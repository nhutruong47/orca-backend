package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.CreateDisputeRequest;
import org.example.backend.dto.OrderDisputeDTO;
import org.example.backend.dto.RespondDisputeRequest;
import org.example.backend.dto.ResolveDisputeRequest;
import org.example.backend.entity.User;
import org.example.backend.service.DisputeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping
    public ResponseEntity<OrderDisputeDTO> openDispute(
            @Valid @RequestBody CreateDisputeRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(disputeService.openDispute(request, currentUser));
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<OrderDisputeDTO> respondDispute(
            @PathVariable UUID id,
            @Valid @RequestBody RespondDisputeRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(disputeService.respondDispute(id, request, currentUser));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<OrderDisputeDTO> resolveDispute(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveDisputeRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(disputeService.resolveDispute(id, request, currentUser));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<OrderDisputeDTO>> getDisputesByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(disputeService.getDisputesByOrder(orderId));
    }
}
