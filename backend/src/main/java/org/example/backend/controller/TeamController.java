package org.example.backend.controller;

import org.example.backend.dto.TeamDTO;
import org.example.backend.entity.User;
import org.example.backend.service.AccessControlService;
import org.example.backend.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final AccessControlService accessControlService;

    public TeamController(TeamService teamService, AccessControlService accessControlService) {
        this.teamService = teamService;
        this.accessControlService = accessControlService;
    }

    @GetMapping
    public ResponseEntity<?> getMyTeams(Authentication auth) {
        return ResponseEntity.ok(teamService.getTeamsForUser(auth.getName()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<TeamDTO>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTeamDetail(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        accessControlService.requireTeamMember(user, id);
        return ResponseEntity.ok(teamService.getTeamDetail(id));
    }

    @PostMapping
    public ResponseEntity<TeamDTO> createTeam(@Valid @RequestBody TeamDTO dto, Authentication auth) {
        return ResponseEntity.ok(teamService.createTeam(dto, auth.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTeam(@PathVariable UUID id, @Valid @RequestBody TeamDTO dto, Authentication auth) {
        return ResponseEntity.ok(teamService.updateTeam(id, dto, auth.getName()));
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinByCode(@RequestBody Map<String, String> body, Authentication auth) {
        String code = body.get("inviteCode");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mã mời không được để trống"));
        }
        TeamDTO team = teamService.joinByCode(code, auth.getName());
        return ResponseEntity.ok(team);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<?> addMember(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        Map<String, String> result = teamService.addMemberByEmail(id, email, auth.getName());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<?> removeMember(
            @PathVariable UUID teamId,
            @PathVariable UUID userId,
            Authentication auth) {
        teamService.removeMember(teamId, userId, auth.getName());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{teamId}/members/{userId}/labels")
    public ResponseEntity<?> updateMemberLabels(
            @PathVariable UUID teamId,
            @PathVariable UUID userId,
            @RequestBody Map<String, List<String>> body,
            @AuthenticationPrincipal User user) {
        accessControlService.requireTeamMember(user, teamId);
        List<String> labels = body.getOrDefault("labels", List.of());
        List<String> updatedLabels = teamService.updateMemberLabels(teamId, userId, labels, user.getUsername());
        return ResponseEntity.ok(updatedLabels);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTeam(@PathVariable UUID id, Authentication auth) {
        teamService.deleteTeam(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/rotate-invite-code")
    public ResponseEntity<?> rotateInviteCode(@PathVariable UUID id, Authentication auth) {
        Map<String, String> result = teamService.rotateInviteCode(id, auth.getName());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/advertise")
    public ResponseEntity<?> advertiseTeam(
            @PathVariable UUID id,
            @RequestBody TeamDTO dto,
            Authentication auth) {
        return ResponseEntity.ok(teamService.advertiseTeam(id, dto, auth.getName()));
    }

    @PutMapping("/{id}/unpublish")
    public ResponseEntity<?> unpublishTeam(@PathVariable UUID id, Authentication auth) {
        teamService.unpublishTeam(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/verification")
    public ResponseEntity<?> submitVerification(
            @PathVariable UUID id,
            @RequestBody TeamDTO dto,
            Authentication auth) {
        return ResponseEntity.ok(teamService.submitVerification(id, dto, auth.getName()));
    }
}
