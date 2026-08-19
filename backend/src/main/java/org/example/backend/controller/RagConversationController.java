package org.example.backend.controller;

import org.example.backend.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Proxy to the AI service's RAG conversation endpoints.
 *
 * Keeps the frontend's traffic internal: it always talks to this
 * backend, never directly to the AI service.
 */
// Disabled: Ver4 RAG conversation proxy was replaced by the stable Ver3 AI workflow.
// Keep the class source for reference, but do not register it as a Spring controller.
@RequestMapping("/api/rag")

public class RagConversationController {

    private static final Logger logger = LoggerFactory.getLogger(RagConversationController.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;

    /**
     * Get conversation messages history.
     * GET /api/rag/conversations/{conversationId}/messages
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<?> getConversationMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "AUTH_REQUIRED"));
        }

        try {
            String url = aiServiceBaseUrl + "/api/rag/conversations/" + conversationId + "/messages?limit=" + limit;
            Map<String, Object> result = restTemplate.getForObject(url, Map.class);
            return ResponseEntity.ok(result);
        } catch (RestClientException exc) {
            logger.warn("Failed to get conversation messages: {}", exc.getMessage());
            return ResponseEntity.status(502).body(Map.of(
                    "error", "AI_SERVICE_ERROR",
                    "message", "Khong the lay lich su cuoc tro chuyen."
            ));
        }
    }

    /**
     * Clear/delete conversation.
     * DELETE /api/rag/conversations/{conversationId}
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<?> clearConversation(
            @PathVariable String conversationId,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "AUTH_REQUIRED"));
        }

        try {
            String url = aiServiceBaseUrl + "/api/rag/conversations/" + conversationId;
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    new HttpEntity<>(jsonHeaders()),
                    Map.class
            );
            return ResponseEntity.ok(resp.getBody());
        } catch (RestClientException exc) {
            logger.warn("Failed to clear conversation: {}", exc.getMessage());
            return ResponseEntity.status(502).body(Map.of(
                    "error", "AI_SERVICE_ERROR",
                    "message", "Khong the xoa cuoc tro chuyen."
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
