package org.example.backend.controller;

import org.example.backend.dto.LoginRequest;
import org.example.backend.dto.RegisterRequest;
import org.example.backend.dto.ChangePasswordRequest;
import org.example.backend.dto.ResetPasswordRequest;
import org.example.backend.dto.UpdateProfileRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
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

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal User user, @RequestBody UpdateProfileRequest request) {
        try {
            return ResponseEntity.ok(authService.updateProfile(user, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal User user,
            @RequestBody ChangePasswordRequest request) {
        try {
            authService.changePassword(user, request);
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(
            @AuthenticationPrincipal User user,
            @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(user, request);
            return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId().toString());
        info.put("username", user.getUsername());
        info.put("fullName", user.getFullName() != null ? user.getFullName() : "");
        info.put("email", user.getEmail() != null ? user.getEmail() : "");
        info.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
        info.put("role", user.getRole().name());
        info.put("chipId", user.getChipId() != null ? user.getChipId() : "");
        info.put("aiPlan", user.getAiPlan() != null ? user.getAiPlan() : "free");
        info.put("aiPlanExpiresAt", user.getAiPlanExpiresAt() != null ? user.getAiPlanExpiresAt().toString() : null);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/trial-status")
    public ResponseEntity<?> getTrialStatus(@AuthenticationPrincipal User user) {
        Map<String, Object> status = new HashMap<>();
        status.put("aiTrialActive", true); // Bỏ chặn UI frontend
        status.put("daysRemaining", user.getAiTrialDaysRemaining());
        status.put("aiUsageCount", user.getAiUsageCount());
        status.put("aiPlan", user.getAiPlan() != null ? user.getAiPlan() : "free");
        
        int maxUsage = 10;
        if ("enterprise".equalsIgnoreCase(user.getAiPlan())) {
            maxUsage = -1; // unlimited
        } else if ("professional".equalsIgnoreCase(user.getAiPlan()) || "plus".equalsIgnoreCase(user.getAiPlan())) {
            maxUsage = 100;
        }
        status.put("aiMaxUsage", maxUsage);
        return ResponseEntity.ok(status);
    }
}
