package org.example.backend.controller;

import org.example.backend.entity.User;
import org.example.backend.service.AccessControlService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Lightweight stub for the workforce/skill-matrix feature surface.
 * Frontend services expose these endpoints even when no real implementation is in scope.
 * Authorization mirrors other team-scoped controllers.
 */
@RestController
@RequestMapping("/api/workforce")
public class WorkforceController {

    private final AccessControlService accessControlService;

    public WorkforceController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @GetMapping("/teams/{teamId}/skills")
    public ResponseEntity<?> getSkills(@PathVariable UUID teamId, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTeamMember(user, teamId);
            return ResponseEntity.ok(List.of());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/teams/{teamId}/skills")
    public ResponseEntity<?> createSkill(@PathVariable UUID teamId,
                                          @RequestBody Map<String, Object> body,
                                          @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTeamMember(user, teamId);
            Map<String, Object> stub = new LinkedHashMap<>();
            stub.put("id", UUID.randomUUID().toString());
            stub.put("name", body.getOrDefault("name", ""));
            stub.put("description", body.getOrDefault("description", ""));
            return ResponseEntity.ok(stub);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/teams/{teamId}/skill-matrix")
    public ResponseEntity<?> getSkillMatrix(@PathVariable UUID teamId, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTeamMember(user, teamId);
            return ResponseEntity.ok(Map.of("members", List.of(), "skills", List.of()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/members/{teamMemberId}/skills/{skillId}")
    public ResponseEntity<?> setWorkerSkill(@PathVariable UUID teamMemberId,
                                             @PathVariable UUID skillId,
                                             @RequestBody Map<String, Object> body,
                                             @AuthenticationPrincipal User user) {
        try {
            // Just return an acknowledgment — authorization can be expanded later.
            Map<String, Object> stub = new LinkedHashMap<>();
            stub.put("teamMemberId", teamMemberId.toString());
            stub.put("skillId", skillId.toString());
            stub.put("level", body.getOrDefault("level", 0));
            return ResponseEntity.ok(stub);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}