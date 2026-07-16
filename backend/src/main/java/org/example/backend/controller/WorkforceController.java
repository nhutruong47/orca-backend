package org.example.backend.controller;

import org.example.backend.entity.User;
import org.example.backend.service.AccessControlService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stub workforce / skill-matrix surface.
 *
 * Frontend services still call these endpoints while the skill-matrix feature
 * is being designed. They are intentionally read-only stubs that echo the
 * request so that the UI can be developed against a stable contract without
 * pretending to mutate state that doesn't exist yet. Authorization mirrors
 * other team-scoped controllers.
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

    @GetMapping("/teams/{teamId}/skill-matrix")
    public ResponseEntity<?> getSkillMatrix(@PathVariable UUID teamId, @AuthenticationPrincipal User user) {
        try {
            accessControlService.requireTeamMember(user, teamId);
            return ResponseEntity.ok(Map.of("members", List.of(), "skills", List.of()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
