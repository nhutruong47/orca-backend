package org.example.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.backend.dto.ai.StandardizedAiResponseDTO;
import org.example.backend.entity.AiPlan;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.repository.AiPlanRepository;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists AI-generated plans and orchestrates the call to the AI service.
 *
 * Lifecycle states: DRAFT, REVISED, APPROVED, REJECTED, PROMOTED, EXPIRED.
 * Allowed transitions are enforced by {@link #isAllowedTransition(String, String)}.
 */
@Service
public class AiPlanService {

    private static final Logger logger = LoggerFactory.getLogger(AiPlanService.class);

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final AiPlanRepository planRepo;
    private final TeamRepository teamRepo;
    private final UserRepository userRepo;
    private final TeamMemberRepository teamMemberRepo;
    private final AccessControlService accessControlService;
    private final RestTemplate restTemplate;

    @Value("${ai.service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;

    public AiPlanService(AiPlanRepository planRepo,
                         TeamRepository teamRepo,
                         UserRepository userRepo,
                         TeamMemberRepository teamMemberRepo,
                         AccessControlService accessControlService) {
        this.planRepo = planRepo;
        this.teamRepo = teamRepo;
        this.userRepo = userRepo;
        this.teamMemberRepo = teamMemberRepo;
        this.accessControlService = accessControlService;
        this.restTemplate = new RestTemplate();
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public List<AiPlan> listForTeam(UUID teamId, User requester) {
        accessControlService.requireTeamMember(requester, teamId);
        return planRepo.findByTeam_IdOrderByUpdatedAtDesc(teamId);
    }

    public AiPlan get(UUID planId, User requester) {
        AiPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("AI plan not found: " + planId));
        accessControlService.requireTeamMember(requester, plan.getTeam().getId());
        return plan;
    }

    // ------------------------------------------------------------------
    // Generation
    // ------------------------------------------------------------------

    public AiPlan generateDraft(UUID teamId, User user, String query, String conversationId) {
        accessControlService.requireTeamMember(user, teamId);
        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        StandardizedAiResponseDTO ai = callAssistant(
                query, teamId.toString(), user.getId().toString(), conversationId);

        AiPlan plan = new AiPlan();
        plan.setTeam(team);
        plan.setOwner(user);
        plan.setSourceQuery(query);
        plan.setIntent("RAG_ASSISTANT");
        plan.setGoalTitle(ai.getAnswer() != null ? truncate(ai.getAnswer(), 240) : "Kế hoạch AI");
        plan.setReasoningSummary(ai.getReasoningSummary());
        plan.setStatus("DRAFT");
        plan.setConversationId(conversationId);
        plan.setReferencedKnowledgeJson(toJson(ai.getReferencedKnowledge()));
        plan.setSuggestedActionsJson(toJson(ai.getSuggestedActions()));
        if (ai.getConfidence() != null && ai.getConfidence().get("score") instanceof Number n) {
            plan.setConfidenceScore(n.doubleValue());
        }

        return planRepo.save(plan);
    }

    public AiPlan generatePlan(UUID teamId, User user, String query, String intent, Map<String, Object> fields) {
        accessControlService.requireTeamMember(user, teamId);
        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("teamId", teamId.toString());
        body.put("intent", intent);
        body.put("fields", fields);

        Map<String, Object> aiResponse;
        try {
            aiResponse = callAi("/plan", body, Map.class);
        } catch (Exception exc) {
            logger.warn("AI /plan failed, persisting raw query as fallback: {}", exc.getMessage());
            aiResponse = new HashMap<>();
        }

        AiPlan plan = new AiPlan();
        plan.setTeam(team);
        plan.setOwner(user);
        plan.setSourceQuery(query);
        plan.setIntent(intent);
        plan.setStatus("DRAFT");
        plan.setGoalTitle(stringOrNull(aiResponse.get("goalTitle"), "Kế hoạch AI"));
        plan.setOutputTarget(stringOrNull(aiResponse.get("outputTarget"), null));
        plan.setPriority(intOr(aiResponse.get("priority"), 3));

        Object deadline = aiResponse.get("deadline");
        if (deadline != null) {
            try {
                plan.setDeadline(LocalDateTime.parse(deadline.toString()));
            } catch (Exception ignored) {
                // free-form date string — leave null
            }
        }

        Object tasks = aiResponse.get("tasks");
        plan.setTasksJson(toJson(tasks));

        return planRepo.save(plan);
    }

    // ------------------------------------------------------------------
    // Status transitions
    // ------------------------------------------------------------------

    public AiPlan updateStatus(UUID planId, String newStatus, User requester) {
        AiPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("AI plan not found: " + planId));
        accessControlService.requireTeamMember(requester, plan.getTeam().getId());
        ensureOwnerOrAdmin(plan, requester);

        String current = plan.getStatus();
        if (!isAllowedTransition(current, newStatus)) {
            throw new IllegalStateException(
                    "Illegal status transition: " + current + " -> " + newStatus);
        }
        plan.setStatus(newStatus);
        if ("APPROVED".equals(newStatus)) {
            plan.setApprovedAt(LocalDateTime.now());
        }
        return planRepo.save(plan);
    }

    public AiPlan markPromoted(UUID planId, UUID goalId, User requester) {
        AiPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("AI plan not found: " + planId));
        accessControlService.requireTeamMember(requester, plan.getTeam().getId());
        ensureOwnerOrAdmin(plan, requester);

        if (!"APPROVED".equals(plan.getStatus())) {
            throw new IllegalStateException(
                    "Only APPROVED plans can be promoted (current: " + plan.getStatus() + ")");
        }
        plan.setStatus("PROMOTED");
        plan.setPromotedGoalId(goalId);
        return planRepo.save(plan);
    }

    public AiPlan revise(UUID planId, User requester, String instruction) {
        AiPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("AI plan not found: " + planId));
        accessControlService.requireTeamMember(requester, plan.getTeam().getId());
        ensureOwnerOrAdmin(plan, requester);

        if ("PROMOTED".equals(plan.getStatus()) || "REJECTED".equals(plan.getStatus())) {
            throw new IllegalStateException("Cannot revise a finalized plan.");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("teamId", plan.getTeam().getId().toString());
        body.put("instruction", instruction);
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("goalTitle", plan.getGoalTitle());
        draft.put("outputTarget", plan.getOutputTarget());
        draft.put("deadline", plan.getDeadline() != null ? plan.getDeadline().toString() : null);
        draft.put("priority", plan.getPriority());
        draft.put("tasks", parseJson(plan.getTasksJson()));
        body.put("draft", draft);

        try {
            Map<String, Object> revised = callAi("/revise", body, Map.class);
            if (revised != null) {
                if (revised.get("goalTitle") != null) {
                    plan.setGoalTitle(revised.get("goalTitle").toString());
                }
                if (revised.get("outputTarget") != null) {
                    plan.setOutputTarget(revised.get("outputTarget").toString());
                }
                if (revised.get("tasks") != null) {
                    plan.setTasksJson(toJson(revised.get("tasks")));
                }
                if (revised.get("priority") instanceof Number n) {
                    plan.setPriority(n.intValue());
                }
            }
        } catch (Exception exc) {
            logger.warn("AI /revise failed, keeping the current plan untouched: {}", exc.getMessage());
        }
        plan.setStatus("REVISED");
        return planRepo.save(plan);
    }

    public AiPlan updateTasks(UUID planId, User requester, String tasksJson) {
        AiPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("AI plan not found: " + planId));
        accessControlService.requireTeamMember(requester, plan.getTeam().getId());
        ensureOwnerOrAdmin(plan, requester);
        plan.setTasksJson(tasksJson);
        plan.setStatus("REVISED");
        return planRepo.save(plan);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static boolean isAllowedTransition(String from, String to) {
        if (to == null) return false;
        return switch (from) {
            case "DRAFT" -> to.equals("REVISED") || to.equals("APPROVED") || to.equals("REJECTED");
            case "REVISED" -> to.equals("APPROVED") || to.equals("REJECTED") || to.equals("DRAFT");
            case "APPROVED" -> to.equals("PROMOTED") || to.equals("REJECTED");
            case "REJECTED", "PROMOTED", "EXPIRED" -> false;
            default -> false;
        };
    }

    private void ensureOwnerOrAdmin(AiPlan plan, User requester) {
        if (requester == null) {
            throw new SecurityException("Authentication required");
        }
        org.example.backend.entity.Role requesterRole = requester.getRole();
        if (requesterRole == org.example.backend.entity.Role.ADMIN) return;
        if (!plan.getOwner().getId().equals(requester.getId())) {
            throw new SecurityException("Only the plan owner can modify it.");
        }
    }

    private StandardizedAiResponseDTO callAssistant(String query, String teamId, String userId, String conversationId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        body.put("team_id", teamId);
        body.put("user_id", userId);
        body.put("conversation_id", conversationId);
        body.put("max_documents", 5);

        try {
            ResponseEntity<StandardizedAiResponseDTO> resp = restTemplate.exchange(
                    aiServiceBaseUrl + "/api/ai/assistant/query",
                    HttpMethod.POST,
                    new HttpEntity<>(body, jsonHeaders()),
                    StandardizedAiResponseDTO.class
            );
            StandardizedAiResponseDTO dto = resp.getBody();
            if (dto == null) {
                throw new IllegalStateException("Empty response from AI service");
            }
            return dto;
        } catch (RestClientException exc) {
            logger.warn("AI service call failed: {}", exc.getMessage());
            StandardizedAiResponseDTO fallback = new StandardizedAiResponseDTO();
            fallback.setAnswer("Không thể kết nối AI service. Vui lòng thử lại sau.");
            fallback.setReasoningSummary("AI service unreachable: " + exc.getMessage());
            fallback.setConfidence(Map.of("score", 0.0, "level", "low", "reasons", List.of("ai_service_down")));
            return fallback;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T callAi(String path, Object body, Class<T> type) {
        ResponseEntity<T> resp = restTemplate.exchange(
                aiServiceBaseUrl + path,
                HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders()),
                type
        );
        return resp.getBody();
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "ORCA-Backend/1.0");
        return headers;
    }

    private static String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException exc) {
            logger.warn("Failed to serialize to JSON: {}", exc.getMessage());
            return null;
        }
    }

    private static Object parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, Object.class);
        } catch (Exception exc) {
            return null;
        }
    }

    private static String truncate(String text, int n) {
        if (text == null) return null;
        return text.length() <= n ? text : text.substring(0, n - 1) + "…";
    }

    private static String stringOrNull(Object value, String fallback) {
        if (value == null) return fallback;
        return value.toString();
    }

    private static Integer intOr(Object value, int fallback) {
        if (value instanceof Number n) return n.intValue();
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException exc) {
            return fallback;
        }
    }

    public AiPlanRepository getPlanRepo() { return planRepo; }
    public UserRepository getUserRepo() { return userRepo; }
    public TeamMemberRepository getTeamMemberRepo() { return teamMemberRepo; }
}