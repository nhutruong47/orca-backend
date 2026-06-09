package org.example.backend.controller;

import org.example.backend.dto.LoginRequest;
import org.example.backend.dto.RegisterRequest;
import org.example.backend.entity.User;
import org.example.backend.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.register(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (AuthenticationException e) {
            log.warn("Login failed for username '{}'", request != null ? request.getUsername() : null);
            return ResponseEntity.status(401).body(Map.of("error", "Tai khoan hoac mat khau khong dung!"));
        } catch (Exception e) {
            log.error("Login failed due to server error for username '{}'", request != null ? request.getUsername() : null, e);
            return ResponseEntity.status(500).body(Map.of("error", "Khong the dang nhap do loi he thong."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId().toString());
        info.put("username", user.getUsername());
        info.put("fullName", user.getFullName() != null ? user.getFullName() : "");
        info.put("email", user.getEmail() != null ? user.getEmail() : "");
        info.put("role", user.getRole().name());
        info.put("chipId", user.getChipId() != null ? user.getChipId() : "");
        info.put("aiPlan", user.getAiPlan() != null ? user.getAiPlan() : "free");
        info.put("aiPlanExpiresAt", user.getAiPlanExpiresAt() != null ? user.getAiPlanExpiresAt().toString() : null);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/trial-status")
    public ResponseEntity<?> getTrialStatus(@AuthenticationPrincipal User user) {
        Map<String, Object> status = new HashMap<>();
        status.put("aiTrialActive", user.isAiTrialActive());
        status.put("daysRemaining", user.getAiTrialDaysRemaining());
        return ResponseEntity.ok(status);
    }
}
