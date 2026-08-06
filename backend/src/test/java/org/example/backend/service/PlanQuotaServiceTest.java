package org.example.backend.service;

import org.example.backend.dto.PlanUsageDTO;
import org.example.backend.entity.Role;
import org.example.backend.entity.Team;
import org.example.backend.entity.User;
import org.example.backend.exception.PlanLimitExceededException;
import org.example.backend.repository.SubscriptionPlanRepository;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlanQuotaServiceTest {

    private SubscriptionPlanRepository planRepository;
    private TeamRepository teamRepository;
    private TeamMemberRepository teamMemberRepository;
    private PlanQuotaService service;

    @BeforeEach
    void setUp() {
        planRepository = mock(SubscriptionPlanRepository.class);
        teamRepository = mock(TeamRepository.class);
        teamMemberRepository = mock(TeamMemberRepository.class);
        service = new PlanQuotaService(planRepository, teamRepository, teamMemberRepository);
        when(planRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void freePlanBlocksSecondOwnedWorkshop() {
        User owner = ownerWithPlan("free");
        when(teamRepository.countByOwnerId(owner.getId())).thenReturn(1L);
        when(teamMemberRepository.countDistinctUsersByTeamOwnerId(owner.getId())).thenReturn(1L);

        PlanLimitExceededException error = assertThrows(
                PlanLimitExceededException.class,
                () -> service.requireWorkshopSlot(owner));

        assertEquals("WORKSHOPS", error.getLimitType());
        assertEquals(1, error.getCurrent());
        assertEquals(1, error.getLimit());
    }

    @Test
    void plusPlanAllowsFifthOwnedWorkshop() {
        User owner = ownerWithPlan("plus");
        when(teamRepository.countByOwnerId(owner.getId())).thenReturn(4L);
        when(teamMemberRepository.countDistinctUsersByTeamOwnerId(owner.getId())).thenReturn(12L);

        assertDoesNotThrow(() -> service.requireWorkshopSlot(owner));
    }

    @Test
    void freePlanBlocksNewUniqueMemberAtCapacity() {
        User owner = ownerWithPlan("free");
        User candidate = ownerWithPlan("free");
        Team team = teamOwnedBy(owner);
        when(teamRepository.countByOwnerId(owner.getId())).thenReturn(1L);
        when(teamMemberRepository.countDistinctUsersByTeamOwnerId(owner.getId())).thenReturn(3L);
        when(teamMemberRepository.existsByUserIdAndTeamOwnerId(candidate.getId(), owner.getId()))
                .thenReturn(false);

        PlanLimitExceededException error = assertThrows(
                PlanLimitExceededException.class,
                () -> service.requireMemberSlot(team, candidate));

        assertEquals("USERS", error.getLimitType());
        assertEquals(3, error.getCurrent());
        assertEquals(3, error.getLimit());
    }

    @Test
    void memberAlreadyCountedForOwnerCanJoinAnotherOwnedWorkshopAtCapacity() {
        User owner = ownerWithPlan("free");
        User candidate = ownerWithPlan("free");
        Team team = teamOwnedBy(owner);
        when(teamMemberRepository.existsByUserIdAndTeamOwnerId(candidate.getId(), owner.getId()))
                .thenReturn(true);

        assertDoesNotThrow(() -> service.requireMemberSlot(team, candidate));
    }

    @Test
    void expiredEnterprisePlanUsesFreeLimits() {
        User owner = ownerWithPlan("enterprise");
        owner.setAiPlanExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(teamRepository.countByOwnerId(owner.getId())).thenReturn(1L);
        when(teamMemberRepository.countDistinctUsersByTeamOwnerId(owner.getId())).thenReturn(2L);

        PlanUsageDTO usage = service.getUsage(owner);

        assertEquals("free", usage.planId());
        assertEquals(3, usage.maxUsers());
        assertEquals(1, usage.maxWorkshops());
    }

    private User ownerWithPlan(String plan) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("user-" + UUID.randomUUID())
                .password("secret")
                .role(Role.MEMBER)
                .build();
        user.setAiPlan(plan);
        return user;
    }

    private Team teamOwnedBy(User owner) {
        Team team = new Team();
        team.setId(UUID.randomUUID());
        team.setName("Roastery");
        team.setOwner(owner);
        return team;
    }
}
