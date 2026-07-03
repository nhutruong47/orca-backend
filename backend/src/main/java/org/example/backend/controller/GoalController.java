package org.example.backend.controller;

import org.example.backend.dto.GoalDTO;
import org.example.backend.entity.User;
import org.example.backend.service.AccessControlService;
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

    @Autowired
    private AccessControlService accessControlService;

    /** Lấy goals theo team (chỉ member mới được đọc) */
    @GetMapping
    public ResponseEntity<?> getByTeam(@RequestParam UUID teamId, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTeamMember(user, teamId);
            return ResponseEntity.ok(goalService.getByTeam(teamId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Tạo goal (chỉ Group Owner) */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody GoalDTO dto, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(goalService.create(dto, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireGoalAccess(user, id);
            return ResponseEntity.ok(goalService.getDetail(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireGoalAccess(user, id);
            return ResponseEntity.ok(goalService.updateStatus(id, body.get("status")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireGoalAccess(user, id);
            goalService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa mục tiêu"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
