package org.example.backend.controller;

import org.example.backend.dto.AiParseResult;
import org.example.backend.entity.TeamMember;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.service.AccessControlService;
import org.example.backend.service.AiServiceClient;
import org.example.backend.service.AiUsageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiServiceClient aiServiceClient;
    private final TeamMemberRepository teamMemberRepo;
    private final AccessControlService accessControlService;
    private final AiUsageService aiUsageService;

    public AiController(AiServiceClient aiServiceClient,
                        TeamMemberRepository teamMemberRepo,
                        AccessControlService accessControlService,
                        AiUsageService aiUsageService) {
        this.aiServiceClient = aiServiceClient;
        this.teamMemberRepo = teamMemberRepo;
        this.accessControlService = accessControlService;
        this.aiUsageService = aiUsageService;
    }

    /**
     * Frontend gọi trực tiếp để xem kết quả AI parse trước khi tạo Goal.
     * Giờ sẽ gửi kèm danh sách thành viên + nhãn dán để AI giao việc ngay.
     *
     * Authorization: user phải thuộc team mới được đọc member labels.
     */
    @PostMapping("/parse")
    public ResponseEntity<AiParseResult> parseText(@RequestBody Map<String, String> payload,
                                                   @AuthenticationPrincipal org.example.backend.entity.User user) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }
        aiUsageService.enforceAndIncrementUsage(user);

        String text = payload.getOrDefault("text", "");
        String teamIdStr = payload.get("teamId");
        if (text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        java.util.UUID teamId = null;
        if (teamIdStr != null && !teamIdStr.isBlank()) {
            try { teamId = java.util.UUID.fromString(teamIdStr); } catch (Exception ignored) {}
        }

        if (teamId != null) {
            accessControlService.validateTeamAccess(user.getId(), teamId);
        }

        String memberContext = buildMemberContext(teamId);
        String history = payload.get("history");
        AiParseResult result = aiServiceClient.parseTask(text, teamId, memberContext, history);
        return ResponseEntity.ok(result);
    }

    private String buildMemberContext(java.util.UUID teamId) {
        if (teamId == null) {
            return "";
        }
        List<TeamMember> members = teamMemberRepo.findByTeamId(teamId);
        StringBuilder sb = new StringBuilder();
        for (TeamMember tm : members) {
            String name = tm.getUser().getUsername();
            List<String> labels = tm.getJobLabels();
            sb.append("- ").append(name);
            if (labels != null && !labels.isEmpty()) {
                sb.append(" (Nhãn: ").append(String.join(", ", labels)).append(")");
            } else {
                sb.append(" (Chưa gán nhãn)");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
