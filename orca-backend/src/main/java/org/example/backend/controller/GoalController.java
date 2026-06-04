package org.example.backend.controller;

import org.example.backend.dto.GoalDTO;
import org.example.backend.entity.User;
import org.example.backend.service.GoalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    @Autowired
    private GoalService goalService;

    /** Lấy goals theo team */
    @GetMapping
    public ResponseEntity<List<GoalDTO>> getByTeam(@RequestParam UUID teamId) {
        return ResponseEntity.ok(goalService.getByTeam(teamId));
    }

    /** Tạo goal (chỉ Group Owner) */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody GoalDTO dto, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(goalService.create(dto, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", friendlyError(e)));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(goalService.updateStatus(id, body.get("status"), user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", friendlyError(e)));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        try {
            goalService.delete(id, user);
            return ResponseEntity.ok(Map.of("message", "Đã xóa mục tiêu"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", friendlyError(e)));
        }
    }

    private String friendlyError(RuntimeException e) {
        String message = e.getMessage() != null ? e.getMessage() : "";
        String lower = message.toLowerCase();
        if (lower.contains("could not execute statement") || lower.contains("constraint") || lower.contains("public.") || lower.contains("sql")) {
            return "Khong the luu ke hoach do du lieu bi trung hoac khong hop le. Vui long thu tao lai.";
        }
        return message.isBlank() ? "Khong the xu ly yeu cau. Vui long thu lai." : message;
    }
}
