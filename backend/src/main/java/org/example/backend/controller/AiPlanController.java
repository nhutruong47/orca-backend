package org.example.backend.controller;

import org.example.backend.entity.User;
import org.example.backend.service.AccessControlService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Lightweight stub for the AI plan surface referenced by the frontend's aiPlanService.
 * Authorization is enforced via AccessControlService to mirror the rest of the codebase.
 */
@RestController
@RequestMapping("/api/ai-plans")
public class AiPlanController {

    private final AccessControlService accessControlService;

    public AiPlanController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @GetMapping("/teams/{teamId}")
    public ResponseEntity<?> getByTeam(@PathVariable UUID teamId, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTeamMember(user, teamId);
            return ResponseEntity.ok(List.of());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/teams/{teamId}")
    public ResponseEntity<?> createDraft(@PathVariable UUID teamId,
                                          @RequestBody Map<String, Object> body,
                                          @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTeamMember(user, teamId);
            Map<String, Object> stub = new LinkedHashMap<>();
            stub.put("id", UUID.randomUUID().toString());
            stub.put("teamId", teamId.toString());
            stub.put("status", "DRAFT");
            stub.put("data", body);
            return ResponseEntity.ok(stub);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{planId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID planId,
                                          @RequestBody Map<String, Object> body,
                                          @AuthenticationPrincipal User user) {
        try {
            Map<String, Object> stub = new LinkedHashMap<>();
            stub.put("id", planId.toString());
            stub.put("status", body.getOrDefault("status", "DRAFT"));
            return ResponseEntity.ok(stub);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}