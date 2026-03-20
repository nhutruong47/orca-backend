package org.example.backend.controller;

import org.example.backend.dto.TeamDTO;
import org.example.backend.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired
    private TeamService teamService;

    /** Lấy danh sách nhóm của user hiện tại */
    @GetMapping
    public ResponseEntity<List<TeamDTO>> getMyTeams(Authentication auth) {
        return ResponseEntity.ok(teamService.getTeamsForUser(auth.getName()));
    }

    /** Lấy danh sách tất cả các nhóm (cho Marketplace) */
    @GetMapping("/all")
    public ResponseEntity<List<TeamDTO>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    /** Xem chi tiết nhóm */
    @GetMapping("/{id}")
    public ResponseEntity<TeamDTO> getTeamDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(teamService.getTeamDetail(id));
    }

    /** Tạo nhóm mới */
    @PostMapping
    public ResponseEntity<TeamDTO> createTeam(@RequestBody TeamDTO dto, Authentication auth) {
        return ResponseEntity.ok(teamService.createTeam(dto, auth.getName()));
    }

    /** Tham gia nhóm bằng Invite Code */
    @PostMapping("/join")
    public ResponseEntity<?> joinByCode(@RequestBody Map<String, String> body, Authentication auth) {
        String code = body.get("inviteCode");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mã mời không được để trống"));
        }
        try {
            TeamDTO team = teamService.joinByCode(code, auth.getName());
            return ResponseEntity.ok(team);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<Map<String, String>> addMember(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        try {
            Map<String, String> result = teamService.addMemberByEmail(id, email, auth.getName());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Xóa thành viên (chỉ Owner) */
    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID teamId,
            @PathVariable UUID userId,
            Authentication auth) {
        teamService.removeMember(teamId, userId, auth.getName());
        return ResponseEntity.ok().build();
    }

    /** Cập nhật nhãn công việc của thành viên */
    @PutMapping("/{teamId}/members/{userId}/labels")
    public ResponseEntity<List<String>> updateMemberLabels(
            @PathVariable UUID teamId,
            @PathVariable UUID userId,
            @RequestBody Map<String, List<String>> body,
            Authentication auth) {
        List<String> labels = body.getOrDefault("labels", List.of());
        List<String> updatedLabels = teamService.updateMemberLabels(teamId, userId, labels, auth.getName());
        return ResponseEntity.ok(updatedLabels);
    }

    /** Xóa nhóm (chỉ Owner) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable UUID id, Authentication auth) {
        teamService.deleteTeam(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    /** Bật quảng cáo (Publish) */
    @PutMapping("/{id}/advertise")
    public ResponseEntity<TeamDTO> advertiseTeam(
            @PathVariable UUID id,
            @RequestBody TeamDTO dto,
            Authentication auth) {
        return ResponseEntity.ok(teamService.advertiseTeam(id, dto, auth.getName()));
    }

    /** Tắt quảng cáo (Unpublish) */
    @PutMapping("/{id}/unpublish")
    public ResponseEntity<Void> unpublishTeam(@PathVariable UUID id, Authentication auth) {
        teamService.unpublishTeam(id, auth.getName());
        return ResponseEntity.ok().build();
    }
}
