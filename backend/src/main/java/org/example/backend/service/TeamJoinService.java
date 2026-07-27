package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.TeamJoinDecisionRequest;
import org.example.backend.dto.TeamJoinRequestDTO;
import org.example.backend.entity.GroupRole;
import org.example.backend.entity.Team;
import org.example.backend.entity.TeamJoinRequest;
import org.example.backend.entity.TeamMember;
import org.example.backend.entity.User;
import org.example.backend.repository.TeamJoinRequestRepository;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamJoinService {

    private final TeamJoinRequestRepository joinRepo;
    private final TeamRepository teamRepo;
    private final UserRepository userRepo;
    private final TeamMemberRepository teamMemberRepo;
    private final NotificationService notificationService;

    @Transactional
    public TeamJoinRequestDTO joinByCode(String inviteCode, String username) {
        Team team = teamRepo.findByInviteCode(inviteCode.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Mã mời không hợp lệ hoặc không tồn tại"));

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (teamMemberRepo.findByTeamIdAndUserId(team.getId(), user.getId()).isPresent()) {
            throw new RuntimeException("Bạn đã là thành viên của nhóm này rồi");
        }

        if (joinRepo.findByTeamIdAndUserIdAndStatus(team.getId(), user.getId(), "PENDING").isPresent()) {
            throw new RuntimeException("Bạn đã gửi yêu cầu và đang chờ duyệt");
        }

        TeamJoinRequest req = TeamJoinRequest.builder()
                .team(team)
                .user(user)
                .status("PENDING")
                .build();
        
        req = joinRepo.save(req);

        // Notify Owner
        notificationService.createAndSend(
                team.getOwner(),
                "Yêu cầu gia nhập xưởng",
                user.getFullName() + " muốn tham gia xưởng " + team.getName() + " của bạn.",
                "/groups",
                null
        );

        return toDTO(req);
    }

    @Transactional(readOnly = true)
    public List<TeamJoinRequestDTO> getPendingRequestsForTeam(UUID teamId, String username) {
        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!team.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Only owner can view requests");
        }

        return joinRepo.findByTeamIdAndStatus(teamId, "PENDING").stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TeamJoinRequestDTO makeDecision(UUID requestId, TeamJoinDecisionRequest decisionReq, String username) {
        TeamJoinRequest req = joinRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        User owner = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!req.getTeam().getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException("Only owner can make decision");
        }

        req.setStatus(decisionReq.getDecision().toUpperCase());
        joinRepo.save(req);

        if ("APPROVED".equals(req.getStatus())) {
            TeamMember tm = new TeamMember();
            tm.setTeam(req.getTeam());
            tm.setUser(req.getUser());
            tm.setGroupRole(GroupRole.MEMBER);
            teamMemberRepo.save(tm);

            notificationService.createAndSend(
                    req.getUser(),
                    "Yêu cầu gia nhập được duyệt",
                    "Bạn đã trở thành thành viên của xưởng " + req.getTeam().getName(),
                    "/groups",
                    null
            );
        } else if ("REJECTED".equals(req.getStatus())) {
            notificationService.createAndSend(
                    req.getUser(),
                    "Yêu cầu gia nhập bị từ chối",
                    "Yêu cầu tham gia xưởng " + req.getTeam().getName() + " đã bị từ chối.",
                    "/groups",
                    null
            );
        }

        return toDTO(req);
    }

    private TeamJoinRequestDTO toDTO(TeamJoinRequest req) {
        TeamJoinRequestDTO dto = new TeamJoinRequestDTO();
        dto.setId(req.getId());
        dto.setTeamId(req.getTeam().getId());
        dto.setTeamName(req.getTeam().getName());
        dto.setUserId(req.getUser().getId());
        dto.setUserName(req.getUser().getFullName());
        dto.setUserEmail(req.getUser().getEmail());
        dto.setStatus(req.getStatus());
        dto.setCreatedAt(req.getCreatedAt());
        return dto;
    }
}
