package org.example.backend.controller;

import org.example.backend.entity.User;
import org.example.backend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        return ResponseEntity.ok(adminService.getOverview());
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        return ResponseEntity.ok(adminService.getUsers());
    }

    @GetMapping("/teams")
    public ResponseEntity<List<Map<String, Object>>> getTeams() {
        return ResponseEntity.ok(adminService.getTeams());
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Map<String, Object>>> getOrders() {
        return ResponseEntity.ok(adminService.getOrders());
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<Map<String, Object>>> getTasks() {
        return ResponseEntity.ok(adminService.getTasks());
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(adminService.updateUserRole(id, body.get("role"), currentUser));
    }

    @PatchMapping("/teams/{id}/publication")
    public ResponseEntity<Map<String, Object>> updateTeamPublication(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(adminService.updateTeamPublication(id, Boolean.TRUE.equals(body.get("published"))));
    }

    @PatchMapping("/tasks/{id}/status")
    public ResponseEntity<Map<String, Object>> updateTaskStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.updateTaskStatus(id, body.get("status")));
    }
}
