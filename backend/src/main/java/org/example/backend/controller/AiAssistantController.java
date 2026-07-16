package org.example.backend.controller;

import org.example.backend.entity.User;
import org.example.backend.service.AiUsageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Proxy to the AI service's RAG /assistant/query endpoint.
 *
 * Keeps the frontend's traffic internal: it always talks to this
 * backend, never directly to the AI service. Authentication, AI usage
 * limits, and CORS are enforced here.
 */
@RestController
@RequestMapping("/api/ai/assistant")
public class AiAssistantController {

    private static final Logger logger = LoggerFactory.getLogger(AiAssistantController.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final AiUsageService aiUsageService;

    @Value("${ai.service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;

    public AiAssistantController(AiUsageService aiUsageService) {
        this.aiUsageService = aiUsageService;
    }

    @PostMapping("/query")
    public ResponseEntity<?> query(@RequestBody Map<String, Object> body,
                                   @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "AUTH_REQUIRED"));
        }

        try {
            aiUsageService.enforceAndIncrementUsage(user);
        } catch (Exception exc) {
            return ResponseEntity.status(402).body(Map.of(
                    "error", "PAYMENT_REQUIRED",
                    "message", exc.getMessage() == null ? "AI quota exceeded" : exc.getMessage()
            ));
        }

        String query = body.get("query") instanceof String s ? s : "";
        if (query.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "BAD_REQUEST",
                    "message", "Query is required"
            ));
        }

        Map<String, Object> aiBody = new LinkedHashMap<>();
        aiBody.put("query", query);
        aiBody.put("team_id", body.getOrDefault("team_id", ""));
        aiBody.put("user_id", String.valueOf(user.getId()));
        aiBody.put("conversation_id", body.getOrDefault("conversation_id", ""));
        aiBody.put("max_documents", body.getOrDefault("max_documents", 5));
        Object sources = body.get("sources");
        if (sources != null) {
            aiBody.put("sources", sources);
        }

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    aiServiceBaseUrl + "/api/ai/assistant/query",
                    HttpMethod.POST,
                    new HttpEntity<>(aiBody, jsonHeaders()),
                    Map.class
            );
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (RestClientException exc) {
            logger.warn("AI service call failed: {}", exc.getMessage());
            return ResponseEntity.status(502).body(Map.of(
                    "error", "AI_SERVICE_ERROR",
                    "message", "AI service is unreachable. Please try again.",
                    "details", exc.getMessage()
            ));
        }
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("User-Agent", "ORCA-Backend/1.0");
        return h;
    }
}