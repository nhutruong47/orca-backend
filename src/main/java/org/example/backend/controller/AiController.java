package org.example.backend.controller;

import org.example.backend.dto.AiParseResult;
import org.example.backend.entity.TeamMember;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.service.AiServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiServiceClient aiServiceClient;
    private final TeamMemberRepository teamMemberRepo;

    public AiController(AiServiceClient aiServiceClient, TeamMemberRepository teamMemberRepo) {
        this.aiServiceClient = aiServiceClient;
        this.teamMemberRepo = teamMemberRepo;
    }

    /**
     * Frontend gọi trực tiếp để xem kết quả AI parse trước khi tạo Goal.
     * Giờ sẽ gửi kèm danh sách thành viên + nhãn dán để AI giao việc ngay.
     */
    @PostMapping("/parse")
    public ResponseEntity<AiParseResult> parseText(@RequestBody Map<String, String> payload) {
        String text = payload.getOrDefault("text", "");
        String teamIdStr = payload.get("teamId");
        if (text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        java.util.UUID teamId = null;
        if (teamIdStr != null && !teamIdStr.isBlank()) {
            try { teamId = java.util.UUID.fromString(teamIdStr); } catch (Exception ignored) {}
        }

        // Build member context from team members' job labels
        String memberContext = "";
        if (teamId != null) {
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
            memberContext = sb.toString();
        }
        
        AiParseResult result = aiServiceClient.parseTask(text, teamId, memberContext);
        return ResponseEntity.ok(result);
    }
}

