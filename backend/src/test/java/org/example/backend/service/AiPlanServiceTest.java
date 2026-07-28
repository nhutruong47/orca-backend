package org.example.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.dto.ai.AiPlanDraftResponse;
import org.example.backend.dto.ai.AiTaskDraft;
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
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

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
    @DisplayName("saveStructuredDraft() persists the already returned AI draft without another AI call")
    void saveStructuredDraftPersistsReturnedDraft() {
        AiPlanDraftResponse draft = new AiPlanDraftResponse();
        draft.setGoalTitle("Rang 10kg Arabica");
        draft.setOutputTarget("10kg Arabica");
        draft.setDeadline("2026-07-28T17:00:00");
        draft.setPriority(5);

        AiTaskDraft task = new AiTaskDraft();
        task.setTitle("Rang me Arabica");
        task.setDescription("Thuc hien rang theo profile da chot.");
        task.setPriority(5);
        task.setWorkload(2.0);
        draft.setTasks(java.util.List.of(task));

        when(teamRepo.findById(team.getId())).thenReturn(Optional.of(team));
        when(planRepo.save(any(AiPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        AiPlan saved = service.saveStructuredDraft(
                team.getId(), owner, "rang 10kg arabica", "PRODUCTION_PLAN", draft);

        assertThat(saved.getSourceQuery()).isEqualTo("rang 10kg arabica");
        assertThat(saved.getGoalTitle()).isEqualTo("Rang 10kg Arabica");
        assertThat(saved.getOutputTarget()).isEqualTo("10kg Arabica");
        assertThat(saved.getDeadline()).isEqualTo(java.time.LocalDateTime.parse("2026-07-28T17:00:00"));
        assertThat(saved.getPriority()).isEqualTo(5);
        assertThat(saved.getTasksJson()).contains("Rang me Arabica");
        verify(accessControlService).requireTeamMember(owner, team.getId());
        verify(planRepo).save(any(AiPlan.class));
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
    @DisplayName("revise() does not mark REVISED when the AI service fails")
    void reviseDoesNotPersistWhenAiFails() {
        AiPlan plan = persistedPlan("DRAFT");
        plan.setGoalTitle("Draft");
        plan.setOutputTarget("Output");
        plan.setPriority(3);
        plan.setTasksJson("[]");

        when(planRepo.findById(plan.getId())).thenReturn(Optional.of(plan));
        ReflectionTestUtils.setField(service, "aiServiceBaseUrl", "http://ai.test");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://ai.test/revise")).andRespond(withServerError());

        assertThatThrownBy(() -> service.revise(plan.getId(), owner, "them qc"))
                .isInstanceOf(RestClientException.class);

        assertThat(plan.getStatus()).isEqualTo("DRAFT");
        verify(planRepo, never()).save(any(AiPlan.class));
        server.verify();
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
