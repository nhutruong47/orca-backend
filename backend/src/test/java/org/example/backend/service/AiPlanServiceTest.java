package org.example.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.entity.AiPlan;
import org.example.backend.entity.Role;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.repository.AiPlanRepository;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AiPlanService}.
 *
 * Focus: state machine transitions and persistence without calling the real
 * AI service. The HTTP layer is mocked at the RestTemplate level using
 * {@link ReflectionTestUtils} / {@code @InjectMocks}.
 */
@ExtendWith(MockitoExtension.class)
class AiPlanServiceTest {

    @Mock
    private AiPlanRepository planRepo;
    @Mock
    private TeamRepository teamRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private TeamMemberRepository teamMemberRepo;
    @Mock
    private AccessControlService accessControlService;

    @InjectMocks
    private AiPlanService service;

    private Team team;
    private User owner;
    private User outsider;

    @BeforeEach
    void setUp() {
        team = new Team();
        setField(team, "id", UUID.randomUUID());

        owner = new User();
        setField(owner, "id", UUID.randomUUID());
        owner.setRole(Role.MEMBER);
        owner.setUsername("owner");

        outsider = new User();
        setField(outsider, "id", UUID.randomUUID());
        outsider.setRole(Role.MEMBER);
        outsider.setUsername("outsider");
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = findField(target.getClass(), name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> c = type;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignore) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private AiPlan persistedPlan(String status) {
        AiPlan plan = new AiPlan();
        setField(plan, "id", UUID.randomUUID());
        plan.setTeam(team);
        plan.setOwner(owner);
        plan.setStatus(status);
        return plan;
    }

    @Test
    @DisplayName("updateStatus rejects illegal transition DRAFT -> PROMOTED")
    void updateStatusRejectsIllegalTransition() {
        AiPlan plan = persistedPlan("DRAFT");
        when(planRepo.findById(any(UUID.class))).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.updateStatus(plan.getId(), "PROMOTED", owner))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Illegal status transition");
        verify(planRepo, never()).save(any(AiPlan.class));
    }

    @Test
    @DisplayName("updateStatus accepts legal transition DRAFT -> APPROVED")
    void updateStatusAcceptsLegalTransition() {
        AiPlan plan = persistedPlan("DRAFT");
        when(planRepo.findById(any(UUID.class))).thenReturn(Optional.of(plan));
        when(planRepo.save(any(AiPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        AiPlan updated = service.updateStatus(plan.getId(), "APPROVED", owner);

        assertThat(updated.getStatus()).isEqualTo("APPROVED");
        assertThat(updated.getApprovedAt()).isNotNull();
        verify(accessControlService, times(1)).requireTeamMember(owner, team.getId());
        verify(planRepo, times(1)).save(any(AiPlan.class));
    }

    @Test
    @DisplayName("updateStatus rejects finalised state transitions")
    void updateStatusRejectsFromApprovedToDraft() {
        AiPlan plan = persistedPlan("APPROVED");
        when(planRepo.findById(any(UUID.class))).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.updateStatus(plan.getId(), "DRAFT", owner))
                .isInstanceOf(IllegalStateException.class);
        verify(planRepo, never()).save(any(AiPlan.class));
    }

    @Test
    @DisplayName("markPromoted requires APPROVED state")
    void markPromotedRequiresApproved() {
        AiPlan plan = persistedPlan("DRAFT");
        when(planRepo.findById(any(UUID.class))).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.markPromoted(plan.getId(), UUID.randomUUID(), owner))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only APPROVED plans");
        verify(planRepo, never()).save(any(AiPlan.class));
    }

    @Test
    @DisplayName("markPromoted succeeds from APPROVED and records the goal link")
    void markPromotedSucceedsFromApproved() {
        AiPlan plan = persistedPlan("APPROVED");
        UUID goalId = UUID.randomUUID();
        when(planRepo.findById(any(UUID.class))).thenReturn(Optional.of(plan));
        when(planRepo.save(any(AiPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        AiPlan updated = service.markPromoted(plan.getId(), goalId, owner);

        assertThat(updated.getStatus()).isEqualTo("PROMOTED");
        assertThat(updated.getPromotedGoalId()).isEqualTo(goalId);
    }

    @Test
    @DisplayName("markPromoted rejects non-owner non-admin callers")
    void markPromotedEnforcesOwnership() {
        AiPlan plan = persistedPlan("APPROVED");
        when(planRepo.findById(any(UUID.class))).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.markPromoted(plan.getId(), UUID.randomUUID(), outsider))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Only the plan owner");
    }

    @Test
    @DisplayName("revise() rejects PROMOTED/REJECTED plans (terminal states)")
    void reviseRejectsTerminalStates() {
        AiPlan promoted = persistedPlan("PROMOTED");
        AiPlan rejected = persistedPlan("REJECTED");
        when(planRepo.findById(promoted.getId())).thenReturn(Optional.of(promoted));
        when(planRepo.findById(rejected.getId())).thenReturn(Optional.of(rejected));

        assertThatThrownBy(() -> service.revise(promoted.getId(), owner, "add more tasks"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot revise");
        assertThatThrownBy(() -> service.revise(rejected.getId(), owner, "add more tasks"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot revise");
    }

    @Test
    @DisplayName("get() returns the plan only when the caller is a team member")
    void getDelegatesAuthorization() {
        AiPlan plan = persistedPlan("DRAFT");
        when(planRepo.findById(any(UUID.class))).thenReturn(Optional.of(plan));

        AiPlan returned = service.get(plan.getId(), owner);

        assertThat(returned).isSameAs(plan);
        verify(accessControlService).requireTeamMember(owner, team.getId());
    }

    @Test
    @DisplayName("get() throws when the plan does not exist")
    void getThrowsForMissingPlan() {
        when(planRepo.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(UUID.randomUUID(), owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AI plan not found");
    }

    @Test
    @DisplayName("updateTasks() persists the new task JSON and flips status to REVISED")
    void updateTasksPersistsAndFlipsStatus() throws Exception {
        AiPlan plan = persistedPlan("DRAFT");
        when(planRepo.findById(any(UUID.class))).thenReturn(Optional.of(plan));
        when(planRepo.save(any(AiPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        ObjectMapper mapper = new ObjectMapper();
        String tasksJson = mapper.writeValueAsString(java.util.List.of(
                java.util.Map.of("title", "task-1", "priority", 3, "workload", 2.0)
        ));

        AiPlan updated = service.updateTasks(plan.getId(), owner, tasksJson);

        assertThat(updated.getStatus()).isEqualTo("REVISED");
        assertThat(updated.getTasksJson()).isEqualTo(tasksJson);
    }

    @Test
    @DisplayName("listForTeam() delegates authorization to AccessControlService")
    void listForTeamDelegatesAuthorization() {
        when(planRepo.findByTeam_IdOrderByUpdatedAtDesc(team.getId()))
                .thenReturn(java.util.List.of());

        var result = service.listForTeam(team.getId(), owner);

        assertThat(result).isEmpty();
        verify(accessControlService).requireTeamMember(owner, team.getId());
    }
}
