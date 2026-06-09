package org.example.backend.controller;

import io.jsonwebtoken.Claims;
import org.example.backend.entity.GroupRole;
import org.example.backend.entity.Team;
import org.example.backend.entity.TeamMember;
import org.example.backend.entity.User;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.UserRepository;
import org.example.backend.security.JwtUtil;
import org.example.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
public class TeamInvitationController {

        @Autowired
        private TeamRepository teamRepository;

        @Autowired
        private TeamMemberRepository teamMemberRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private JwtUtil jwtUtil;

        @Autowired
        private EmailService emailService;

        @PostMapping("/{teamId}/invite")
        public ResponseEntity<?> inviteMember(@PathVariable UUID teamId,
                        @RequestBody Map<String, String> payload,
                        @AuthenticationPrincipal UserDetails userDetails) {
                String email = payload.get("email");
                String roleStr = payload.getOrDefault("role", "MEMBER");

                User inviter = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new RuntimeException("Team not found"));

                // Generate invitation token
                String token = jwtUtil.generateInviteToken(email, teamId, roleStr);

                // Link for the frontend
                String inviteLink = "http://localhost:5173/invite?token=" + token;

                // Send email
                emailService.sendInvitationEmail(email, team.getName(),
                                inviter.getFullName() != null ? inviter.getFullName() : inviter.getUsername(),
                                inviteLink);

                return ResponseEntity.ok(Map.of("message", "Invitation sent successfully"));
        }

        @PostMapping("/invites/accept")
        public ResponseEntity<?> acceptInvite(@RequestBody Map<String, String> payload,
                        @AuthenticationPrincipal UserDetails userDetails) {
                String token = payload.get("token");
                Claims claims = jwtUtil.parseInviteToken(token);

                UUID teamId = UUID.fromString(claims.get("teamId", String.class));
                GroupRole role = GroupRole.valueOf(claims.get("groupRole", String.class));

                User user = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Determine if user email matches invitation email.
                // We might want to allow it even if login is different, or strict match.
                // For now, let's just make sure user is logged in.

                Team team = teamRepository.findById(teamId)
                                .orElseThrow(() -> new RuntimeException("Team not found"));

                if (teamMemberRepository.findByTeamIdAndUserId(teamId, user.getId()).isPresent()) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("error", "User is already a member of this team"));
                }

                TeamMember tm = new TeamMember();
                tm.setTeam(team);
                tm.setUser(user);
                tm.setGroupRole(role);
                teamMemberRepository.save(tm);

                return ResponseEntity.ok(Map.of("message", "Joined team successfully", "teamId", teamId));
        }
}
