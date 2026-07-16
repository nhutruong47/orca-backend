package org.example.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.entity.AiPlan;
import org.example.backend.entity.User;
import org.example.backend.service.AiPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Production AI plan controller.
 *
 * Contract:
 *   GET    /api/ai-plans/teams/{teamId}     — list plans for a team
 *   GET    /api/ai-plans/{planId}           — fetch one plan
 *   POST   /api/ai-plans/teams/{teamId}     — generate a draft (RAG or /plan)
 *   POST   /api/ai-plans/{planId}/revise    — revise a draft
 *   PATCH  /api/ai-plans/{planId}/status    — change status (DRAFT/APPROVED/...)
 *   POST   /api/ai-plans/{planId}/promote   — mark as PROMOTED, linking to a Goal
 */
@RestController
@RequestMapping("/api/ai-plans")
public class AiPlanController {

    private final AiPlanService aiPlanService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiPlanController(AiPlanService aiPlanService) {
        this.aiPlanService = aiPlanService;
    }

    @GetMapping("/teams/{teamId}")
    public ResponseEntity<?> getByTeam(@PathVariable UUID teamId,
                                      @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "AUTH_REQUIRED"));
        }
        try {
            return ResponseEntity.ok(aiPlanService.listForTeam(teamId, user));
        } catch (SecurityException exc) {
            return ResponseEntity.status(403).body(Map.of("error", "FORBIDDEN", "message", exc.getMessage()));
        } catch (IllegalArgumentException exc) {
            return ResponseEntity.badRequest().body(Map.of("error", "BAD_REQUEST", "message", exc.getMessage()));
        }
    }

    @GetMapping("/{planId}")
    public ResponseEntity<?> getOne(@PathVariable UUID planId,
                                    @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "AUTH_REQUIRED"));
        }
        try {
            return ResponseEntity.ok(aiPlanService.get(planId, user));
        } catch (SecurityException exc) {
            return ResponseEntity.status(403).body(Map.of("error", "FORBIDDEN", "message", exc.getMessage()));
        } catch (IllegalArgumentException exc) {
            return ResponseEntity.status(404).body(Map.of("error", "NOT_FOUND", "message", exc.getMessage()));
        }
    }

    @PostMapping("/teams/{teamId}")
    public ResponseEntity<?> createDraft(@PathVariable UUID teamId,
                                         @RequestBody Map<String, Object> body,
                                         @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "AUTH_REQUIRED"));
        }

        String query = stringOrNull(body.get("query"));
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "BAD_REQUEST",
                    "message", "'query' must be a non-empty string."));
        }

        String intent = stringOrNull(body.get("intent"));
        String conversationId = stringOrNull(body.get("conversationId"));
        Object fieldsObj = body.get("fields");

        try {
            AiPlan plan;
            if (intent != null && fieldsObj instanceof Map<?, ?> fields) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fieldsCast = (Map<String, Object>) fields;
                plan = aiPlanService.generatePlan(teamId, user, query, intent, fieldsCast);
            } else {
                plan = aiPlanService.generateDraft(teamId, user, query, conversationId);
            }
            return ResponseEntity.status(201).body(plan);
        } catch (SecurityException exc) {
            return ResponseEntity.status(403).body(Map.of("error", "FORBIDDEN", "message", exc.getMessage()));
        } catch (IllegalStateException exc) {
            return ResponseEntity.status(409).body(Map.of("error", "CONFLICT", "message", exc.getMessage()));
        } catch (IllegalArgumentException exc) {
            return ResponseEntity.badRequest().body(Map.of("error", "BAD_REQUEST", "message", exc.getMessage()));
        } catch (Exception exc) {
            return ResponseEntity.status(502).body(Map.of(
                    "error", "AI_SERVICE_ERROR",
                    "message", exc.getMessage() == null ? "AI service failure" : exc.getMessage()));
        }
    }

    @PostMapping("/{planId}/revise")
    public ResponseEntity<?> revise(@PathVariable UUID planId,
                                   @RequestBody Map<String, Object> body,
                                   @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "AUTH_REQUIRED"));
        }
        String instruction = stringOrNull(body.get("instruction"));
        if (instruction == null || instruction.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "BAD_REQUEST",
                    "message", "'instruction' must be a non-empty string."));
        }
        try {
            AiPlan updated = aiPlanService.revise(planId, user, instruction);
            return ResponseEntity.ok(updated);
        } catch (SecurityException exc) {
            return ResponseEntity.status(403).body(Map.of("error", "FORBIDDEN", "message", exc.getMessage()));
        } catch (IllegalStateException exc) {
            return ResponseEntity.status(409).body(Map.of("error", "CONFLICT", "message", exc.getMessage()));
        } catch (IllegalArgumentException exc) {
            return ResponseEntity.badRequest().body(Map.of("error", "BAD_REQUEST", "message", exc.getMessage()));
        }
    }

    @PatchMapping("/{planId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID planId,
                                          @RequestBody Map<String, Object> body,
                                          @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "AUTH_REQUIRED"));
        }
        String newStatus = stringOrNull(body.get("status"));
        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "BAD_REQUEST",
                    "message", "'status' must be provided."));
        }
        try {
            AiPlan updated = aiPlanService.updateStatus(planId, newStatus, user);
            return ResponseEntity.ok(updated);
        } catch (SecurityException exc) {
            return ResponseEntity.status(403).body(Map.of("error", "FORBIDDEN", "message", exc.getMessage()));
        } catch (IllegalStateException exc) {
            return ResponseEntity.status(409).body(Map.of("error", "CONFLICT", "message", exc.getMessage()));
        } catch (IllegalArgumentException exc) {
            return ResponseEntity.badRequest().body(Map.of("error", "BAD_REQUEST", "message", exc.getMessage()));
        }
    }

    @PostMapping("/{planId}/promote")
    public ResponseEntity<?> promote(@PathVariable UUID planId,
                                     @RequestBody Map<String, Object> body,
                                     @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "AUTH_REQUIRED"));
        }
        Object goalIdRaw = body.get("goalId");
        UUID goalId;
        try {
            goalId = goalIdRaw == null ? null : UUID.fromString(goalIdRaw.toString());
        } catch (Exception exc) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "BAD_REQUEST",
                    "message", "'goalId' must be a UUID."));
        }
        try {
            AiPlan updated = aiPlanService.markPromoted(planId, goalId, user);
            return ResponseEntity.ok(updated);
        } catch (SecurityException exc) {
            return ResponseEntity.status(403).body(Map.of("error", "FORBIDDEN", "message", exc.getMessage()));
        } catch (IllegalStateException exc) {
            return ResponseEntity.status(409).body(Map.of("error", "CONFLICT", "message", exc.getMessage()));
        } catch (IllegalArgumentException exc) {
            return ResponseEntity.badRequest().body(Map.of("error", "BAD_REQUEST", "message", exc.getMessage()));
        }
    }

    private static String stringOrNull(Object o) {
        if (o == null) return null;
        String s = o.toString();
        return s.isBlank() ? null : s;
    }
}