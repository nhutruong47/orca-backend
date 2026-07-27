package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.AdminVerifyRequest;
import org.example.backend.dto.SubmitVerificationRequest;
import org.example.backend.dto.VerificationRequestDTO;
import org.example.backend.entity.User;
import org.example.backend.repository.UserRepository;
import org.example.backend.service.VerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;
    private final UserRepository userRepository;

    @PostMapping("/submit")
    public ResponseEntity<VerificationRequestDTO> submitVerification(
            @Valid @RequestBody SubmitVerificationRequest request,
            Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(verificationService.submitVerification(request, user));
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VerificationRequestDTO>> getPendingVerifications() {
        return ResponseEntity.ok(verificationService.getPendingVerifications());
    }

    @PostMapping("/admin/{requestId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VerificationRequestDTO> reviewVerification(
            @PathVariable UUID requestId,
            @Valid @RequestBody AdminVerifyRequest request) {
        return ResponseEntity.ok(verificationService.reviewVerification(requestId, request));
    }
}
