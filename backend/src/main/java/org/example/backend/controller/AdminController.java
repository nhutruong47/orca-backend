package org.example.backend.controller;

import org.example.backend.entity.User;
import org.example.backend.entity.SubscriptionPlan;
import org.example.backend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @GetMapping("/payments")
    public ResponseEntity<List<Map<String, Object>>> getPayments() {
        return ResponseEntity.ok(adminService.getPayments());
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.createUser(body));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.updateUser(id, body));
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetUserPassword(@PathVariable UUID id) {
        String newPassword = adminService.resetUserPassword(id);
        return ResponseEntity.ok(Map.of("password", newPassword));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(adminService.updateUserRole(id, body.get("role"), currentUser));
    }

    @PatchMapping("/users/{id}/lock")
    public ResponseEntity<Map<String, Object>> updateUserLock(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(adminService.updateUserLock(id, Boolean.TRUE.equals(body.get("locked")), currentUser));
    }

    @PutMapping("/teams/{id}")
    public ResponseEntity<Map<String, Object>> updateTeam(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.updateTeam(id, body));
    }

    @DeleteMapping("/teams/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable UUID id) {
        adminService.deleteTeam(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/teams/{id}/publication")
    public ResponseEntity<Map<String, Object>> updateTeamPublication(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(adminService.updateTeamPublication(id, Boolean.TRUE.equals(body.get("published"))));
    }

    @PatchMapping("/teams/{id}/verification")
    public ResponseEntity<Map<String, Object>> updateTeamVerification(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.updateTeamVerification(id, body.get("status"), body.get("rejectReason")));
    }

    @PatchMapping("/tasks/{id}/status")
    public ResponseEntity<Map<String, Object>> updateTaskStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.updateTaskStatus(id, body.get("status")));
    }

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getPlans() {
        return ResponseEntity.ok(adminService.getPlans());
    }

    @PostMapping("/plans")
    public ResponseEntity<SubscriptionPlan> createPlan(@RequestBody SubscriptionPlan plan) {
        return ResponseEntity.ok(adminService.createPlan(plan));
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<SubscriptionPlan> updatePlan(
            @PathVariable UUID id,
            @RequestBody SubscriptionPlan plan) {
        return ResponseEntity.ok(adminService.updatePlan(id, plan));
    }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable UUID id) {
        adminService.deletePlan(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ai-configs")
    public ResponseEntity<Map<String, String>> getAiConfigs() {
        return ResponseEntity.ok(adminService.getAiConfigs());
    }

    @PutMapping("/ai-configs")
    public ResponseEntity<Void> updateAiConfigs(@RequestBody Map<String, String> configs) {
        adminService.updateAiConfigs(configs);
        return ResponseEntity.ok().build();
    }
}
