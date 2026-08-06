package org.example.backend.controller;

import org.example.backend.dto.ai.AiExtractRequest;
import org.example.backend.dto.ai.AiPlanRequest;
import org.example.backend.dto.ai.AiReviseRequest;
import org.example.backend.entity.User;
import org.example.backend.service.AiWorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/v2")
public class AiWorkflowController {

    private final AiWorkflowService aiWorkflowService;

    public AiWorkflowController(AiWorkflowService aiWorkflowService) {
        this.aiWorkflowService = aiWorkflowService;
    }

    @PostMapping("/extract")
    public ResponseEntity<?> extract(@RequestBody AiExtractRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(aiWorkflowService.extract(request, user));
    }

    @PostMapping("/plan")
    public ResponseEntity<?> plan(@RequestBody AiPlanRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(aiWorkflowService.plan(request, user));
    }

    @PostMapping("/revise")
    public ResponseEntity<?> revise(@RequestBody AiReviseRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(aiWorkflowService.revise(request, user));
    }
}
