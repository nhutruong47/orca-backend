package org.example.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.dto.ai.AiExtractRequest;
import org.example.backend.dto.ai.AiPlanDraftResponse;
import org.example.backend.dto.ai.AiPlanRequest;
import org.example.backend.dto.ai.AiReviseRequest;
import org.example.backend.entity.AiPlan;
import org.example.backend.entity.User;
import org.example.backend.service.AiPlanService;
import org.example.backend.service.AiUsageService;
import org.example.backend.service.AiWorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/v2")
public class AiWorkflowController {

    private final AiWorkflowService aiWorkflowService;
    private final AiUsageService aiUsageService;
    private final AiPlanService aiPlanService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiWorkflowController(AiWorkflowService aiWorkflowService,
                                AiUsageService aiUsageService,
                                AiPlanService aiPlanService) {
        this.aiWorkflowService = aiWorkflowService;
        this.aiUsageService = aiUsageService;
        this.aiPlanService = aiPlanService;
    }

    @PostMapping("/extract")
    public ResponseEntity<?> extract(@RequestBody AiExtractRequest request, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "AUTH_REQUIRED"));
        }
        try {
            aiUsageService.enforceAndIncrementUsage(user);
            return ResponseEntity.ok(aiWorkflowService.extract(request, user));
        } catch (org.springframework.web.server.ResponseStatusException exc) {
            return ResponseEntity.status(exc.getStatusCode()).body(Map.of("error", exc.getReason()));
        } catch (Exception exc) {
            return ResponseEntity.status(502).body(Map.of("error", "AI_SERVICE_ERROR", "message", exc.getMessage()));
        }
    }

    /**
     * Generates a structured plan AND persists it as an AiPlan draft.
     * The response contains both the structured draft and the persisted
     * plan id so the frontend can immediately route the user to the
     * drafts list.
     */
    @PostMapping("/plan")
    public ResponseEntity<?> plan(@RequestBody AiPlanRequest request, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "AUTH_REQUIRED"));
        }
        try {
            aiUsageService.enforceAndIncrementUsage(user);
            AiPlanDraftResponse draft = aiWorkflowService.plan(request, user);
            Map<String, Object> fieldsMap = null;
            if (request.getFields() != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cast = objectMapper.convertValue(request.getFields(), Map.class);
                    fieldsMap = cast;
                } catch (Exception ignored) {
                    fieldsMap = Map.of("raw", request.getFields());
                }
            }
            UUID teamId;
            try {
                teamId = UUID.fromString(request.getTeamId());
            } catch (Exception exc) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "BAD_REQUEST", "message", "teamId must be a UUID"));
            }
            String sourceQuery = fieldsMap != null && fieldsMap.get("text") instanceof String s
                    ? s
                    : (fieldsMap != null && fieldsMap.get("query") instanceof String s2 ? s2 : "");
            AiPlan persisted = aiPlanService.generatePlan(
                    teamId, user, sourceQuery,
                    request.getIntent() == null ? "UNKNOWN" : request.getIntent(),
                    fieldsMap == null ? Map.of() : fieldsMap);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("draft", draft);
            out.put("planId", persisted.getId().toString());
            out.put("planStatus", persisted.getStatus());
            return ResponseEntity.ok(out);
        } catch (org.springframework.web.server.ResponseStatusException exc) {
            return ResponseEntity.status(exc.getStatusCode()).body(Map.of("error", exc.getReason()));
        } catch (IllegalStateException exc) {
            return ResponseEntity.status(409).body(Map.of("error", "CONFLICT", "message", exc.getMessage()));
        } catch (Exception exc) {
            return ResponseEntity.status(502).body(Map.of("error", "AI_SERVICE_ERROR", "message", exc.getMessage()));
        }
    }

    @PostMapping("/revise")
    public ResponseEntity<?> revise(@RequestBody AiReviseRequest request, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "AUTH_REQUIRED"));
        }
        try {
            aiUsageService.enforceAndIncrementUsage(user);
            AiPlanDraftResponse draft = aiWorkflowService.revise(request, user);
            return ResponseEntity.ok(draft);
        } catch (org.springframework.web.server.ResponseStatusException exc) {
            return ResponseEntity.status(exc.getStatusCode()).body(Map.of("error", exc.getReason()));
        } catch (Exception exc) {
            return ResponseEntity.status(502).body(Map.of("error", "AI_SERVICE_ERROR", "message", exc.getMessage()));
        }
    }
}