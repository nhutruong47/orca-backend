package org.example.backend.security;

import org.example.backend.entity.GroupRole;
import org.example.backend.entity.Role;
import org.example.backend.entity.Team;
import org.example.backend.entity.TeamMember;
import org.example.backend.entity.User;
import org.example.backend.repository.AttendanceRepository;
import org.example.backend.repository.DailyTargetRepository;
import org.example.backend.repository.GoalRepository;
import org.example.backend.repository.InterGroupOrderRepository;
import org.example.backend.repository.InventoryRepository;
import org.example.backend.repository.ProductionOrderRepository;
import org.example.backend.repository.ProductionPlanRepository;
import org.example.backend.repository.TaskChecklistRepository;
import org.example.backend.repository.TaskRepository;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.repository.UserRepository;
import org.example.backend.service.AccessControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Security tests for {@link AccessControlService}.
 *
 * Central authorization logic that gates every team-scoped endpoint:
 *
 *  - ADMIN users always have access.
 *  - Team members (any role) have access via {@code team_member} rows.
 *  - Outsiders get a forbidden exception.
 *  - Missing principal always fails.
 */
@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock private TeamMemberRepository teamMemberRepo;
    @Mock private UserRepository userRepo;
    @Mock private ProductionOrderRepository productionOrderRepo;
    @Mock private InterGroupOrderRepository interGroupOrderRepo;
    @Mock private InventoryRepository inventoryRepo;
    @Mock private GoalRepository goalRepo;
    @Mock private TaskRepository taskRepo;
    @Mock private TaskChecklistRepository taskChecklistRepo;
    @Mock private AttendanceRepository attendanceRepo;
    @Mock private ProductionPlanRepository productionPlanRepo;
    @Mock private DailyTargetRepository dailyTargetRepo;

    @InjectMocks
    private AccessControlService service;

    private User admin;
    private User member;
    private User outsider;
    private Team team;
    private TeamMember membership;

    @BeforeEach
    void setUp() throws Exception {
        admin = makeUser(UUID.randomUUID(), Role.ADMIN, "admin");
        member = makeUser(UUID.randomUUID(), Role.MEMBER, "member");
        outsider = makeUser(UUID.randomUUID(), Role.MEMBER, "outsider");

        team = new Team();
        setField(team, "id", UUID.randomUUID());

        membership = new TeamMember();
        setField(membership, "groupRole", GroupRole.MEMBER);
    }

    private static User makeUser(UUID id, Role role, String username) throws Exception {
        User u = new User();
        setField(u, "id", id);
        u.setRole(role);
        u.setUsername(username);
        return u;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ignore) {
                c = c.getSuperclass();
            }
        }
    }

    @Test
    @DisplayName("ADMIN user is always allowed, even with no membership row")
    void adminAlwaysAllowed() {
        when(userRepo.findById(admin.getId())).thenReturn(Optional.of(admin));

        // Should not throw.
        service.requireTeamMember(admin, team.getId());

        // teamMemberRepo should never be queried for an admin short-circuit.
        org.mockito.Mockito.verify(teamMemberRepo, org.mockito.Mockito.never())
                .existsByTeamIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("Team member is allowed through requireTeamMember")
    void teamMemberAllowed() {
        when(userRepo.findById(member.getId())).thenReturn(Optional.of(member));
        when(teamMemberRepo.existsByTeamIdAndUserId(team.getId(), member.getId()))
                .thenReturn(true);

        service.requireTeamMember(member, team.getId());
    }

    @Test
    @DisplayName("Outsider (no membership) is denied")
    void outsiderDenied() {
        when(userRepo.findById(outsider.getId())).thenReturn(Optional.of(outsider));
        when(teamMemberRepo.existsByTeamIdAndUserId(team.getId(), outsider.getId()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.requireTeamMember(outsider, team.getId()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    @DisplayName("Null principal always fails authorization")
    void nullPrincipalDenied() {
        assertThatThrownBy(() -> service.requireTeamMember((User) null, team.getId()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    @DisplayName("Null team id always fails authorization")
    void nullTeamIdDenied() {
        assertThatThrownBy(() -> service.requireTeamMember(member, null))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    @DisplayName("requireTeamAdmin denies non-admin members")
    void requireTeamAdminDeniesMembers() {
        when(userRepo.findById(member.getId())).thenReturn(Optional.of(member));
        when(teamMemberRepo.findByTeamIdAndUserId(team.getId(), member.getId()))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.requireTeamAdmin(member, team.getId()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    @DisplayName("requireTeamAdmin allows a team ADMIN (groupRole=ADMIN)")
    void requireTeamAdminAllowsAdmins() throws Exception {
        TeamMember adminMembership = new TeamMember();
        setField(adminMembership, "groupRole", GroupRole.ADMIN);

        User groupAdmin = makeUser(UUID.randomUUID(), Role.MEMBER, "group-admin");
        when(userRepo.findById(groupAdmin.getId())).thenReturn(Optional.of(groupAdmin));
        when(teamMemberRepo.findByTeamIdAndUserId(team.getId(), groupAdmin.getId()))
                .thenReturn(Optional.of(adminMembership));

        // Should not throw.
        service.requireTeamAdmin(groupAdmin, team.getId());
    }
}
