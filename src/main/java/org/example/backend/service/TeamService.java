package org.example.backend.service;

import org.example.backend.dto.TeamDTO;
import org.example.backend.entity.*;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.UserRepository;
import org.example.backend.repository.TaskRepository;
import org.example.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Lấy tất cả nhóm mà user tham gia
     */
    public List<TeamDTO> getTeamsForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<TeamMember> memberships = teamMemberRepository.findByUserId(user.getId());
        return memberships.stream()
                .map(tm -> toDTO(tm.getTeam(), false))
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả nhóm trên hệ thống có đăng quảng cáo (cho Marketplace)
     */
    public List<TeamDTO> getAllTeams() {
        return teamRepository.findAll().stream()
                .filter(Team::isPublished)
                .map(t -> toDTO(t, false))
                .collect(Collectors.toList());
    }

    /**
     * Xem chi tiết nhóm (bao gồm danh sách thành viên)
     */
    public TeamDTO getTeamDetail(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        return toDTO(team, true);
    }

    /**
     * Tạo nhóm mới — user trở thành Owner
     */
    @Transactional
    public TeamDTO createTeam(TeamDTO dto, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Team team = new Team();
        team.setName(dto.getName());
        team.setDescription(dto.getDescription());
        team.setOwner(owner);
        team = teamRepository.save(team);

        // Thêm owner vào team_members với role ADMIN
        TeamMember ownerMember = new TeamMember();
        ownerMember.setTeam(team);
        ownerMember.setUser(owner);
        ownerMember.setGroupRole(GroupRole.ADMIN);
        teamMemberRepository.save(ownerMember);

        return toDTO(team, false);
    }

    /**
     * Thêm thành viên bằng EMAIL:
     * - Nếu email đã có tài khoản: thêm vào nhóm ngay
     * - Nếu chưa có tài khoản: gửi email mời có link
     */
    @Transactional
    public Map<String, String> addMemberByEmail(UUID teamId, String email, String requesterUsername) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        checkAdminRole(team, requester);

        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User member = existingUser.get();

            if (teamMemberRepository.findByTeamIdAndUserId(teamId, member.getId()).isPresent()) {
                throw new RuntimeException("Đã là thành viên của nhóm này rồi!");
            }

            TeamMember tm = new TeamMember();
            tm.setTeam(team);
            tm.setUser(member);
            tm.setGroupRole(GroupRole.MEMBER);
            teamMemberRepository.save(tm);

            return Map.of(
                    "status", "ADDED",
                    "message", "Đã thêm " + member.getUsername() + " vào nhóm!");
        } else {
            String token = jwtUtil.generateInviteToken(email, teamId, "MEMBER");
            String inviteLink = "http://localhost:5173/invite?token=" + token;
            String inviterName = requester.getFullName() != null ? requester.getFullName() : requester.getUsername();

            boolean sent = emailService.sendInvitationEmail(email, team.getName(), inviterName, inviteLink);

            if (sent) {
                // Gửi email thành công
                return Map.of(
                        "status", "INVITED",
                        "message", "Đã gửi email mời thành công tới " + email);
            } else {
                // Dev mode: chưa có SMTP thật, hiển link để copy
                return Map.of(
                        "status", "INVITED",
                        "message", "Chưa có email SMTP, copy link bên dưới để gửi thủ công",
                        "inviteLink", inviteLink);
            }
        }
    }

    /**
     * Xóa thành viên khỏi nhóm
     */
    @Transactional
    public void removeMember(UUID teamId, UUID userId, String requesterUsername) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        checkAdminRole(team, requester);

        // Không cho xóa owner/admin tạo ra team (tránh mất kiểm soát)
        if (team.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Cannot remove the group owner");
        }

        TeamMember tm = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found in group"));
        teamMemberRepository.delete(tm);
    }

    /**
     * Cập nhật danh sách nhãn dán (Job Labels) cho thành viên (chỉ Owner/Admin).
     */
    @Transactional
    public List<String> updateMemberLabels(UUID teamId, UUID userId, List<String> labels, String requesterUsername) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        // Yêu cầu quyền ADMIN
        checkAdminRole(team, requester);

        TeamMember tm = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found in group"));

        // Cập nhật nhãn và lưu
        tm.setJobLabels(labels);
        teamMemberRepository.save(tm);

        return tm.getJobLabels();
    }

    /**
     * Xóa nhóm (chỉ Owner)
     */
    @Transactional
    public void deleteTeam(UUID teamId, String requesterUsername) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        checkAdminRole(team, requester);

        // Xóa tất cả members trước
        List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
        teamMemberRepository.deleteAll(members);

        teamRepository.delete(team);
    }

    /**
     * Bật quảng cáo (Publish) Team lên Marketplace
     */
    @Transactional
    public TeamDTO advertiseTeam(UUID teamId, TeamDTO dto, String requesterUsername) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        checkAdminRole(team, requester);

        team.setPublished(true);
        if (dto.getSpecialty() != null)
            team.setSpecialty(dto.getSpecialty());
        if (dto.getCapacity() != null)
            team.setCapacity(dto.getCapacity());
        if (dto.getRegion() != null)
            team.setRegion(dto.getRegion());

        team = teamRepository.save(team);
        return toDTO(team, false);
    }

    /**
     * Tắt quảng cáo (Unpublish) khỏi Marketplace
     */
    @Transactional
    public void unpublishTeam(UUID teamId, String requesterUsername) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        checkAdminRole(team, requester);

        team.setPublished(false);
        teamRepository.save(team);
    }

    /**
     * Tham gia nhóm bằng Invite Code (6 ký tự)
     */
    @Transactional
    public TeamDTO joinByCode(String inviteCode, String username) {
        Team team = teamRepository.findByInviteCode(inviteCode.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Mã mời không hợp lệ hoặc không tồn tại"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if already a member
        if (teamMemberRepository.findByTeamIdAndUserId(team.getId(), user.getId()).isPresent()) {
            throw new RuntimeException("Bạn đã là thành viên của nhóm này rồi");
        }

        TeamMember tm = new TeamMember();
        tm.setTeam(team);
        tm.setUser(user);
        tm.setGroupRole(GroupRole.MEMBER);
        teamMemberRepository.save(tm);

        return toDTO(team, false);
    }

    // === Helper: Entity → DTO ===
    private TeamDTO toDTO(Team team, boolean includeMembers) {
        TeamDTO dto = new TeamDTO();
        dto.setId(team.getId());
        dto.setName(team.getName());
        dto.setDescription(team.getDescription());
        dto.setOwnerId(team.getOwner().getId());
        dto.setOwnerName(team.getOwner().getUsername());
        dto.setCreatedAt(team.getCreatedAt().toString());
        dto.setInviteCode(team.getInviteCode());

        dto.setPublished(team.isPublished());
        dto.setSpecialty(team.getSpecialty());
        dto.setCapacity(team.getCapacity());
        dto.setRegion(team.getRegion());

        // Trust
        dto.setCompletedOrders(team.getCompletedOrders());
        dto.setCancelledOrders(team.getCancelledOrders());
        dto.setTotalOrders(team.getTotalOrders());
        int trust = team.getTotalOrders() > 0
                ? (int) ((double) team.getCompletedOrders() / team.getTotalOrders() * 100)
                : 100;
        dto.setTrustScore(trust);

        List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());
        dto.setMemberCount(members.size());

        if (includeMembers) {
            dto.setMembers(members.stream().map(tm -> {
                TeamDTO.MemberInfo mi = new TeamDTO.MemberInfo();
                mi.setUserId(tm.getUser().getId());
                mi.setUsername(tm.getUser().getUsername());
                mi.setFullName(tm.getUser().getFullName());
                mi.setGroupRole(tm.getGroupRole().name());
                mi.setJoinedAt(tm.getJoinedAt().toString());
                mi.setJobLabels(tm.getJobLabels());

                // Task stats mặc định = 0, sẽ được tính riêng khi cần
                mi.setTotalTasks(0);
                mi.setCompletedTasks(0);
                mi.setCompletionRate(0);

                return mi;
            }).collect(Collectors.toList()));
        }

        return dto;
    }

    private void checkAdminRole(Team team, User requester) {
        TeamMember requesterMember = teamMemberRepository.findByTeamIdAndUserId(team.getId(), requester.getId())
                .orElseThrow(() -> new RuntimeException("Requester is not a member of the group"));
        if (requesterMember.getGroupRole() != GroupRole.ADMIN) {
            throw new RuntimeException("Only group ADMINs can perform this action");
        }
    }

    // Cả ADMIN và MEMBER đều có thể mời thành viên mới
    private void checkAdminOrMember(Team team, User requester) {
        teamMemberRepository.findByTeamIdAndUserId(team.getId(), requester.getId())
                .orElseThrow(() -> new RuntimeException("Requester is not a member of the group"));
        // Không cần kiểm tra role, thành viên nào cũng có thể mời
    }
}
