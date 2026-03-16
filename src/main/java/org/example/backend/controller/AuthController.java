package org.example.backend.controller;

import org.example.backend.dto.AuthResponse;
import org.example.backend.dto.LoginRequest;
import org.example.backend.dto.RegisterRequest;
import org.example.backend.entity.User;
import org.example.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

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
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Tài khoản hoặc mật khẩu không đúng!"));
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
        return ResponseEntity.ok(info);
    }

    /** Kiểm tra trạng thái AI trial */
    @GetMapping("/trial-status")
    public ResponseEntity<?> getTrialStatus(@AuthenticationPrincipal User user) {
        Map<String, Object> status = new HashMap<>();
        status.put("aiTrialActive", user.isAiTrialActive());
        status.put("daysRemaining", user.getAiTrialDaysRemaining());
        return ResponseEntity.ok(status);
    }
}