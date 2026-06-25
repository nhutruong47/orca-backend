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
import org.example.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/ai/v2")
public class AiWorkflowController {

    private final AiWorkflowService aiWorkflowService;
    private final UserRepository userRepository;

    public AiWorkflowController(AiWorkflowService aiWorkflowService, UserRepository userRepository) {
        this.aiWorkflowService = aiWorkflowService;
        this.userRepository = userRepository;
    }

    private void enforceAndIncrementUsage(User user) {
        if (!user.isAiTrialActive()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Hết hạn gói miễn phí. Bạn cần nâng cấp gói để sử dụng tốt hơn."
            );
        }
        user.setAiUsageCount(user.getAiUsageCount() + 1);
        userRepository.save(user);
    }

    @PostMapping("/extract")
    public ResponseEntity<?> extract(@RequestBody AiExtractRequest request, @AuthenticationPrincipal User user) {
        enforceAndIncrementUsage(user);
        return ResponseEntity.ok(aiWorkflowService.extract(request, user));
    }

    @PostMapping("/plan")
    public ResponseEntity<?> plan(@RequestBody AiPlanRequest request, @AuthenticationPrincipal User user) {
        enforceAndIncrementUsage(user);
        return ResponseEntity.ok(aiWorkflowService.plan(request, user));
    }

    @PostMapping("/revise")
    public ResponseEntity<?> revise(@RequestBody AiReviseRequest request, @AuthenticationPrincipal User user) {
        enforceAndIncrementUsage(user);
        return ResponseEntity.ok(aiWorkflowService.revise(request, user));
    }
}
